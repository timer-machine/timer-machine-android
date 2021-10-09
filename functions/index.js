const functions = require('firebase-functions');

const path = require('path');

const admin = require('firebase-admin');
admin.initializeApp()

/**
 * Limit the backup files up to 50.
 * https://googleapis.dev/nodejs/storage/latest/index.html
 */
exports.limitBackup = functions.storage.object().onFinalize(async (object) => {
    // backup/uid/test.txt
    const filePath = object.name;
    // test
    const baseFileName = path.basename(filePath, path.extname(filePath));
    // backup/uid
    const fileDir = path.dirname(filePath);

    const bucket = admin.storage().bucket(object.bucket);
    const getFilesResult = await bucket.getFiles({ directory: fileDir });
    const files = getFilesResult[0];

    if (files.length > 50) {

        var oldest;
        var oldestTime = Number.MAX_VALUE;
        for (const file of files) {
            const getMetaDataResult = await bucket.file(file.name).getMetadata();
            const metaData = getMetaDataResult[0]

            const updated = metaData.updated;
            const updatedDateMilli = Date.parse(updated);

            if (updatedDateMilli < oldestTime) {
                oldest = file;
                oldestTime = updatedDateMilli;
            }
        }

        if (oldest) {
            await oldest.delete()
        }
    }

    return null;
});
