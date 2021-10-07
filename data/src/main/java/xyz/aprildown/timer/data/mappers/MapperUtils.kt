package xyz.aprildown.timer.data.mappers

import xyz.aprildown.timer.domain.Mapper

internal fun <Data, Entity> Data.fromWithMapper(mapper: Mapper<Data, Entity>): Entity =
    mapper.mapFrom(this)

internal fun <Data, Entity> List<Data>.fromWithMapper(mapper: Mapper<Data, Entity>): List<Entity> =
    mapper.mapFrom(this)

internal fun <Data, Entity> Entity.toWithMapper(mapper: Mapper<Data, Entity>): Data =
    mapper.mapTo(this)

internal fun <Data, Entity> List<Entity>.toWithMapper(mapper: Mapper<Data, Entity>): List<Data> =
    mapper.mapTo(this)
