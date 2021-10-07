package xyz.aprildown.timer.data.mappers

import dagger.Reusable
import xyz.aprildown.timer.data.datas.FolderData
import xyz.aprildown.timer.domain.Mapper
import xyz.aprildown.timer.domain.entities.FolderEntity
import javax.inject.Inject

@Reusable
internal class FolderMapper @Inject constructor() : Mapper<FolderData, FolderEntity>() {

    override fun mapFrom(from: FolderData): FolderEntity {
        return FolderEntity(
            id = from.id,
            name = from.name
        )
    }

    override fun mapTo(from: FolderEntity): FolderData {
        return FolderData(
            id = from.id,
            name = from.name
        )
    }
}
