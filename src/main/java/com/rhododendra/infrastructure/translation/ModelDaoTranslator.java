package com.rhododendra.infrastructure.translation;

import com.rhododendra.infrastructure.persisted.*;
import com.rhododendra.model.Botanist;
import com.rhododendra.model.Hybridizer;
import com.rhododendra.model.PhotoDetails;

/**
 * We want to be able to control the JPA classes separately, this class lets us translate these to the model.
 * This has ensures that the model does not depend on the infrastructure, in keeping with Onion Architecture.
 */
public static class ModelDaoTranslator {
    public static Botanist DaoToModel(BotanistDAO b) {
        return new Botanist(
            b.botanicalShorthand,
            b.location,
            b.bornDied,
            b.fullName,
            b.photos.isEmpty() ? null : b.photos.get(0).regularPhotoFileName
        );
    }

    public static Hybridizer DaoToModel(HybridizerDAO h) {
        return new Hybridizer(
            h.oldId,
            h.name,
            h.location,
            h.photos.isEmpty()
                ? null
                : h.photos.stream().map(photoDetailsDAO -> photoDetailsDAO.regularPhotoFileName).toList()
        );
    }

    public static PhotoDetails DaoToModel(PhotoDetailsDAO p){
        return new PhotoDetails(
            p.photoBy,
            p.date,
            p.location,
            p.hiResPhotoFileName,
            p.photoBy,
            p.description,
            p.regularPhotoFileName,
            null
        );
    }

    public static PhotoDetails DaoToModel(RhododendronPhotoDAO r){
        var p =  r.photoDetails;
        return new PhotoDetails(
            p.photoBy,
            p.date,
            p.location,
            p.hiResPhotoFileName,
            p.photoBy,
            p.description,
            p.regularPhotoFileName,
            r.tagPhotoFileName
        );
    }

    public static

}
