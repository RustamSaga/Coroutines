# Другие сценарии

# Валидация текста. Flow + combine

На экране есть два поля ввода: Имя и Возраст.
Валидацию введенных данных будем получать в виде Boolean.

```kotlin
class N30Model : ViewModel() {

    private val _name = MutableStateFlow("")
    private val _age = MutableStateFlow("")

    val dataIsValid: LiveData<Boolean> = combine(_name, _age) { name, age ->
        isNameValid(name) && isAgeValid(age)
    }.asLiveData()

    fun setName(name: String) {
        _name.value = name
    }

    fun setAge(age: String) {
        _age.value = age
    }

    private fun isAgeValid(age: String): Boolean {
        val intAge = age.toInt()
        return age.isNotBlank() && intAge > 18
    }

    private fun isNameValid(name: String): Boolean {
        return name.isNotBlank() && (name.first() == 'A')
    }
}
```

```kotlin
// activity
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.n30_activity)
    nameEditText = findViewById(R.id.nameEditText)
    ageEditText = findViewById(R.id.ageEditText)
    btnGo = findViewById(R.id.btnGo)


    nameEditText.addTextChangedListener { viewModel.setName(it.toString()) }
    ageEditText.addTextChangedListener { viewModel.setAge(it.toString()) }

    btnGo.isEnabled = viewModel.dataIsValid.value == true

}
```

# Фильтр данных. Flow + combine + mapLatest (flatMapLatest)

Реализация
В viewModel снова создаем два StateFlow и методы для передачи данных в них:

```kotlin
private val _name = MutableStateFlow("")
private val _age = MutableStateFlow("")

fun setName(name: String) {
    _name.value = name
}

fun setAge(age: String) {
    _age.value = age
}
```

Вызываем эти методы при изменении текстов:

```kotlin
editTextName.addTextChangedListener { model.setName(it.toString()) }
editTextAge.addTextChangedListener { model.setAge(it.toString()) }
```

В итоге у нас в модели есть два Flow, которые транслируют изменения текста в полях ввода. Мы хотим
эти значения использовать как фильтр в методе запроса данных.

```kotlin
// from UseCase or Repository
suspend fun fetchData(filter: Filter): Data

// or
fun fetchData(filter: Filter): Flow<Data>
```

Т.е. нам надо будет данные из двух Flow упаковывать в объект Filter(name, age) и вызывать метод
fetchData:

```kotlin
//  if use fetchData(filter: Filter): Data - use mapLatest{}
val filteredData: LiveData<Data> = combine(_name, _age) { name, age ->
    Filter(name, age)
}.mapLatest { filter ->
    fetchData(filter)
}.asLiveData()


// if use fetchData(...): Flow<Data> - use flatMapLatest {}
val filteredData: LiveData<Data> = combine(_name, _age) { name, age ->
    Filter(name, age)
}.flatMapLatest { filter ->
    fetchData(filter)
}.asLiveData()
```

Оператор `mapLatest` будет отменять текущий запрос `fetchData`, если пришел новый объект `Filter`
(когда поменялся текст в одном из Flow). А asLiveData конвертирует Flow в LiveData, которую можно
использовать в биндинге для отображения полученных данных.

# Данные сразу из кэша, затем с сервера. Flow

Мы хотим сделать так, чтобы при запросе мы сразу получали данные из кэша, а потом уже свежие данные
с сервера.

```kotlin
fun fetchData(id: Int): Flow<Data> = flow {
    val cachedData = cache.getData(id)
    if (cachedData != null) {
        emit(cachedData)
    }

    val apiData = apiService.fetchData(id)
    cache.putData(id, apiData)
    emit(apiData)
}
```

В этот сценарий можно добавить поддержку State паттерна и если в кэше нет данных то вместо них слать
State.Loading, чтобы экран показал индикатор загрузки, пока идет запрос к серверу.

# Периодическая загрузка данных. Flow.

Необходимо создать Flow, который с определенным интервалом будет получать данные с сервера и
отправлять их нам.

```kotlin
fun fetchDataWithPeriod(): Flow<Data> = flow {
    while (true) {
        val data = apiService.fetchData()
        emit(data)
        delay(10_000)
    }
}
```

Если нужна обработка ошибок, то не забывайте про Flow операторы `catch`, `retry`, `retryWhen`. Либо
просто оборачивайте вызов apiService в `try-catch`.

Если надо получить Flow, который будет работать на несколько получателей, то используйте `shareIn`
или `stateIn`.

# Разовый запрос или обновляемые данные. Flow + emitAll

Room dao

```kotlin
@Query("SELECT * FROM data")
suspend fun getAll(): List<Data>

@Query("SELECT * FROM data")
fun getAllFlow(): Flow<List<Data>>
```

viewModel or repository or useCase

```kotlin
fun getData(): Flow<List<Data>> = flow {
    if (refreshIsEnabled) { // checkbox 
        emitAll(dataDao.getAllFlow()) // отправляет нам актуальные данные из БД
    } else {
        emit(dataDao.getAll()) // данные, полученные из БД однократно
    }
}
```

# Базовые классы для UseCase

suspend UseCase

```kotlin
abstract class UseCase<in P, R>(private val coroutineDispatcher: CoroutineDispatcher) {

    suspend operator fun invoke(parameters: P): Result<R> {
        return try {
            // для смены потока и чтобы ошибки не пошли наверх а были обработаны текущим try-catch
            withContext(coroutineDispatcher) {
                execute(parameters).let {
                    Result.Success(it)
                }
            }
        } catch (e: Exception) {
            Timber.d(e)
            Result.Error(e)
        }
    }

    @Throws(RuntimeException::class)
    protected abstract suspend fun execute(parameters: P): R
}
```
Классу наследнику остается только реализовать suspend метод `execute`

Flow UseCase

```kotlin
abstract class FlowUseCase<in P, R>(private val coroutineDispatcher: CoroutineDispatcher) {
    operator fun invoke(parameters: P): Flow<Result<R>> = execute(parameters)
        .catch { e -> emit(Result.Error(Exception(e))) } // поймает ошибку и отправит ее получателю в упаковке Result.Error
        .flowOn(coroutineDispatcher) // используется для смены потока.

    protected abstract fun execute(parameters: P): Flow<Result<R>>
}
```
Классу наследнику остается только реализовать метод execute, который возвращает Flow.

# ***
suspend функция и Flow для работы с локацией: [статья](https://medium.com/androiddevelopers/simplifying-apis-with-coroutines-and-flow-a6fb65338765)
