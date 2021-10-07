package xyz.aprildown.timer.domain

abstract class Mapper<Data, Entity> {
    open fun mapFrom(from: Data): Entity {
        throw UnsupportedOperationException()
    }

    fun mapFrom(from: List<Data>): List<Entity> {
        return from.map { mapFrom(it) }
    }

    open fun mapTo(from: Entity): Data {
        throw UnsupportedOperationException()
    }

    fun mapTo(from: List<Entity>): List<Data> {
        return from.map { mapTo(it) }
    }
}
