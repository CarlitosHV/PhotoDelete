package com.hardbug.photodelete

import coil3.map.Mapper
import coil3.request.Options
import kotlinx.cinterop.*
import platform.Foundation.*
import platform.Photos.*
import platform.UIKit.*
import platform.CoreGraphics.*

@OptIn(ExperimentalForeignApi::class)
class PHAssetMapper : Mapper<String, UIImage> {

    override fun map(data: String, options: Options): UIImage? {
        val fetchResult = PHAsset.fetchAssetsWithLocalIdentifiers(listOf(data), null)

        if (fetchResult.count.toInt() == 0) return null

        val asset = fetchResult.firstObject as? PHAsset ?: return null
        val imageManager = PHImageManager.defaultManager()

        val requestOptions = PHImageRequestOptions().apply {
            setDeliveryMode(PHImageRequestOptionsDeliveryModeHighQualityFormat)
            setSynchronous(true)
            setResizeMode(PHImageRequestOptionsResizeModeExact)
        }

        var resultImage: UIImage? = null

        imageManager.requestImageForAsset(
            asset,
            targetSize = CGSizeMake(1000.0, 1000.0),
            contentMode = PHImageContentModeAspectFit,
            options = requestOptions,
            resultHandler = { image, info ->
                resultImage = image
            }
        )

        return resultImage
    }
}
