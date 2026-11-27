package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Eaten by Spiders — Avacyn Restored #177 (canonical printing)
 * {2}{G} · Instant
 *
 * Destroy target creature with flying and all Equipment attached to that creature.
 *
 * Soul Nova's shape with destroy in place of exile: the attached Equipment is gathered *before*
 * the creature leaves (its attachment list clears on the zone change), then the creature and the
 * gathered Equipment are destroyed. Gathering first also means the Equipment is still destroyed
 * when the creature survives (indestructible or regenerated).
 */
val EatenBySpiders = card("Eaten by Spiders") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Destroy target creature with flying and all Equipment attached to that creature."

    spell {
        val creature = target(TargetFilter(GameObjectFilter.Creature.withKeyword(Keyword.FLYING)))
        effect = Effects.Pipeline(
            descriptionOverride = "Destroy target creature with flying and all Equipment attached to that creature."
        ) {
            val equipment = gather(
                CardSource.AttachedTo(
                    host = creature,
                    filter = GameObjectFilter.Artifact.withSubtype(Subtype.EQUIPMENT)
                )
            )
            run(Effects.Destroy(creature))
            destroy(equipment)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "177"
        artist = "Slawomir Maniak"
        flavorText = "After sampling many victims, the spider developed a preference for the undead and their accoutrements."
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0efea1b1-f212-4b97-98dd-922f85ab191f.jpg?1783940667"
    }
}
