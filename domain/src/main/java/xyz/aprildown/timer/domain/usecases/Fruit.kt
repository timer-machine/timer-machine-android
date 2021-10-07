package xyz.aprildown.timer.domain.usecases

sealed class Fruit<out R> {

    data class Ripe<out T>(val data: T) : Fruit<T>()
    data class Rotten(val exception: Throwable) : Fruit<Nothing>() {
        constructor(message: String) : this(IllegalStateException(message))
    }

    override fun toString(): String {
        return when (this) {
            is Ripe<*> -> "Success[data=$data]"
            is Rotten -> "Rotten[exception=$exception]"
        }
    }
}
