package com.rhododendra.infrastructure.translation;

import com.rhododendra.infrastructure.persisted.*;
import com.rhododendra.model.Botanist;
import com.rhododendra.model.Hybridizer;
import com.rhododendra.model.PhotoDetails;
import com.rhododendra.model.Rhododendron;
import com.rhododendra.repositories.RhodoRepository;
import com.rhododendra.service.SearchService;
import org.springframework.stereotype.Service;

import static com.rhododendra.infrastructure.persisted.PhotoDetailsEntity.Licence.COPYRIGHT;

/**
 * We want to be able to control the JPA classes separately, this class lets us translate these to the model. This
 * ensures that the model does not depend on the infrastructure, in keeping with Onion Architecture.
 */
@Service
public class ModelEntityTranslator {
    private final RhodoRepository rhodoRepository;

    public ModelEntityTranslator(RhodoRepository rhodoRepository) {
        this.rhodoRepository = rhodoRepository;
    }

    public static Botanist EntityToModel(BotanistEntity b) {
        return new Botanist(
            b.botanicalShorthand,
            b.location,
            b.bornDied,
            b.fullName,
            b.photos.isEmpty() ? null : b.photos.get(0).regularPhotoFileName
        );
    }

    public BotanistEntity modelToEntity(Botanist b, boolean fetchReferencedEntities) {
        return fetchReferencedEntities
            ? rhodoRepository.getBotanistWithOldId(b.primaryIdValue())
            : new BotanistEntity(
            b.primaryIdValue(),
            b.getBotanicalShort(),
            b.getLocation(),
            b.getBornDied(),
            b.getFullName(),
            SearchService
                .getPhotoDetailsById(b.getImage())
                .stream()
                .map(p -> modelToPhotoDetailsEntity(p, fetchReferencedEntities))
                .toList()
        );
    }

    public static Hybridizer EntityToModel(HybridizerEntity h) {
        return new Hybridizer(
            h.oldId,
            h.name,
            h.location,
            h.photos.isEmpty()
                ? null
                : h.photos.stream().map(photoDetailsEntity -> photoDetailsEntity.regularPhotoFileName).toList()
        );
    }

    public HybridizerEntity modelToEntity(Hybridizer h, boolean fetchReferencedEntities) {
        return fetchReferencedEntities
            ? rhodoRepository.getHybridizerWithOldId(h.getId())
            : new HybridizerEntity(
            h.getId(),
            h.getName(),
            h.getLocation(),
            h
                .getPhotos()
                .stream()
                .flatMap(x -> SearchService
                    .getPhotoDetailsById(x)
                    .stream()
                    .map(p -> modelToPhotoDetailsEntity(p, fetchReferencedEntities))
                )
                .toList()
        );
    }

    public static PhotoDetails EntityToModel(PhotoDetailsEntity p) {
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

    public PhotoDetailsEntity modelToPhotoDetailsEntity(PhotoDetails p, boolean fetchReferencedEntities) {
        return fetchReferencedEntities
            ? rhodoRepository.getPhotoDetailsEntity(p.getPhoto())
            : new PhotoDetailsEntity(
            p.getPhoto(),
            p.getHiResPhoto(),
            p.getName(),
            p.getDescription(),
            p.getPhotoBy(),
            p.getDate(),
            p.getLocation(),
            COPYRIGHT
        );
    }

    public static PhotoDetails EntityToModel(RhodoPhotoEntity r) {
        var p = r.photoDetails;
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

    public RhodoPhotoEntity modelToRhodoPhotoEntity(
        PhotoDetails p,
        Rhododendron r,
        boolean fetchReferencedEntities
    ) {
        return fetchReferencedEntities
            ? rhodoRepository.getRhodoPhotoEntity(p.getPhoto())
            : new RhodoPhotoEntity(
            p.getTag(),
            modelToEntity(r),
            modelToPhotoDetailsEntity(p, fetchReferencedEntities)
        );
    }

    public Rhododendron EntityToModel(RhododendronEntity r) {
        return new Rhododendron(
            r.oldId,
            r.name,
            r.tenYearHeight,
            r.bloomTime,
            r.flowerShape,
            r.leafShape,
            r.photoDetails.stream().map(x -> x.photoDetails.regularPhotoFileName).toList(),
            r.commonSynonyms,
            r.hardiness,
            r.deciduous,
            r.colour,
            r.extraInformation,
            new Rhododendron.Hybridizer(
                r.hybridizerInfo.HybridizationDetails,
                r.hybridizerInfo.hybridizer.oldId
            ),
            r.hybridizerInfo.irrcRegistered,
            r.hybridizerInfo.additionalParentageInfo,
            r.hybridizerInfo.selectionSpecies.oldId,
            EntityToModel(r.hybridizerInfo.selectionSpecies),
            r.hybridizerInfo.cultivationSince,
            r.lepedote,
            new Rhododendron.Parentage(
                r.parentage.seedParentFallbackDescription,
                r.parentage.seedParent.oldId,
                r.parentage.pollenParentFallbackDescription,
                r.parentage.pollenParent.oldId
            ),
            new Rhododendron.Taxonomy(
                r.botanicalInfo.taxonomy.subgenus,
                r.botanicalInfo.taxonomy.section,
                r.botanicalInfo.taxonomy.subsection
            ),
            r.botanicalInfo.firstDescribed,
            r.botanicalInfo.firstDescribedBotanists.
                stream()
                .map(x -> x.botanicalShorthand)
                .toList(),
            r.botanicalInfo.originLocation,
            r.botanicalInfo.habit,
            r.botanicalInfo.observedMatureHeight,

            r.botanicalInfo.botanicalSynonyms
                .stream()
                .map(x ->
                    new Rhododendron.Synonym(
                        x.synonym,
                        x.botanistShorthand
                    )
                )
                .toList(),
            r.azaleaGroup,
            r.speciesOrCultivar,
            r.hybridizerInfo.isSpeciesSelection,
            r.botanicalInfo.isNaturalHybrid,
            r.hybridizerInfo.isCultivarGroup,
            r.rhodoCategory
        );
    }

    public RhododendronEntity modelToEntity(Rhododendron r, boolean fetchReferencedEntities) {
        return new RhododendronEntity(
            r.getId(),
            r.getName(),
            r.getTen_year_height(),
            r.getBloom_time(),
            r.getFlower_shape(),
            r.getLeaf_shape(),
            r.getSynonyms(),
            r.getHardiness(),
            r.getDeciduous(),
            r.getColour(),
            r.getExtra_information(),
            r.getLepedote(),
            r.getRhodoCategory(),
            r.getSpeciesOrCultivar(),
            r.getAzalea_group(),
            new RhododendronEntity.Parentage(
                r.getParentage().getSeed_parent(),
                rhodoRepository.getRhodoWithOldId(r.getSeedParentId()),
                r.getParentage().getPollen_parent(),
                rhodoRepository.getRhodoWithOldId(r.getPollenParentId())
            ),
            new RhododendronEntity.HybridizationInfo(
                r.getHybridizer().getHybridizer(),
                r.getIrrc_registered(),
                r.getAdditional_parentage_info(),
                rhodoRepository.getRhodoWithOldId(r.getSpecies_id()),
                r.getCultivation_since(),
                0,
                r.getIs_species_selection(),
                r.isCultivarGroup(),
                rhodoRepository.getHybridizerWithOldId(r.getHybridizer().getHybridizer_id())
            ),
            new RhododendronEntity.BotanicalInfo(
                r.getIs_natural_hybrid(),
                new RhododendronEntity.BotanicalInfo.Taxonomy(
                    r.getTaxonomy().getSubgenus(),
                    r.getTaxonomy().getSection(),
                    r.getTaxonomy().getSubsection()
                ),
                r.getFirst_described(),
                r.getFirst_described_botanists()
                    .stream()
                    .map(rhodoRepository::getBotanistWithOldId)
                    .toList(),
                r.getOrigin_location(),
                r.getHabit(),
                r.getObserved_mature_height(),
                r.getBotanical_synonyms()
                    .stream()
                    .map(s -> {
                            var retrievedBotanicalSynonym = rhodoRepository.getBotanicalSynonymByName(s.synonym());
                            return retrievedBotanicalSynonym != null
                                ? retrievedBotanicalSynonym
                                : new BotanicalSynonymEntity(s.synonym(), s.botanical_shorts());
                        }
                    )
                    .toList()
            ),
            r.getPhotos()
                .stream()
                .flatMap(id -> SearchService
                    .getPhotoDetailsById(id)
                    .stream()
                    .map(p -> modelToRhodoPhotoEntity(p, r, fetchReferencedEntities))
                )
                .toList()
        );
    }


    /*
    TODO
        - I'm going to have rhodos that reference other rhodods athe are not yet inserted
        - I need to do this in a two-pass way:
            1. Add all rhodos without references
            2. SELECT * WHERE oldID==id, then link the returned object
     */

}
