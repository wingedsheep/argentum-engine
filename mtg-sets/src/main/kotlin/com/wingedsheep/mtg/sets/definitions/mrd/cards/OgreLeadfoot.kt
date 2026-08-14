package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ogre Leadfoot — Mirrodin #102
 * {4}{R} · Creature — Ogre · 3/3
 *
 * Whenever this creature becomes blocked by an artifact creature, destroy that creature.
 *
 * Modelled with the *filtered* SELF-binding [Triggers.becomesBlocked] shape (the same one
 * flanking is built on): the filter constrains the **blocker**, and the detector fires the
 * ability once per matching blocker with `triggeringEntityId` set to that blocker. So a gang
 * block by three artifact creatures destroys all three, one trigger each, and
 * [EffectTarget.TriggeringEntity] resolves to the right one every time.
 *
 * The unfiltered [Triggers.BecomesBlocked] would be wrong here — it fires exactly once no matter
 * how many creatures block — and [Triggers.BlocksOrBecomesBlockedBy] would over-trigger, since
 * the printed text covers only the blocked direction, not the Leadfoot blocking something.
 *
 * "Destroy that creature" is a plain [Effects.Destroy] with no regeneration clause, so an
 * artifact creature with a regeneration shield survives.
 */
val OgreLeadfoot = card("Ogre Leadfoot") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Ogre"
    power = 3
    toughness = 3
    oracleText = "Whenever this creature becomes blocked by an artifact creature, destroy that creature."

    triggeredAbility {
        trigger = Triggers.becomesBlocked(filter = GameObjectFilter.ArtifactCreature)
        effect = Effects.Destroy(EffectTarget.TriggeringEntity)
        description = "Whenever this creature becomes blocked by an artifact creature, destroy that creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "102"
        artist = "Heather Hudson"
        flavorText = "When the goblins need more scrap for the Great Furnace, they simply let " +
            "the ogres loose and follow in their wake."
        imageUri = "https://cards.scryfall.io/normal/front/e/3/e3c77744-3a86-46cf-9e0f-5a217a1c08b9.jpg?1783944539"
    }
}
