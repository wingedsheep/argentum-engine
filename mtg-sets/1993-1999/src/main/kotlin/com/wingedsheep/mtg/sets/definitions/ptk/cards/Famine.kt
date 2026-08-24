package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Famine
 * {3}{B}{B}
 * Sorcery
 * Famine deals 3 damage to each creature and each player.
 *
 * One printed sentence, two effects joined by [Effects.Composite]. The board half is
 * [Effects.ForEachInGroup] over `GroupFilter(GameObjectFilter.Creature)` with the damage aimed at
 * [EffectTarget.Self] — the current iteration entity. The player half is the corpus' spelling for a
 * symmetric player sweep: [Effects.ForEachPlayer] over [Player.Each], each iteration rebinding the
 * controller so [EffectTarget.Controller] is the player being processed.
 */
val Famine = card("Famine") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Famine deals 3 damage to each creature and each player."

    spell {
        effect = Effects.Composite(
            Effects.ForEachInGroup(
                GroupFilter(GameObjectFilter.Creature),
                Effects.DealDamage(3, EffectTarget.Self)
            ),
            Effects.ForEachPlayer(
                Player.Each,
                listOf(Effects.DealDamage(3, EffectTarget.Controller))
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "75"
        artist = "Sun Nan"
        flavorText = "\"But it was a year of dearth. People were reduced to eating leaves of jujube trees. Corpses were seen everywhere in the countryside.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/d/8d6c10ca-f6d6-4322-aa17-7e874cb10bb1.jpg"
    }
}
