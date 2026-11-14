package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mizzium Meddler
 * {2}{U}
 * Creature — Vedalken Wizard
 * 1/4
 *
 * Flash
 * When this creature enters, you may change a target of target spell or ability to this creature.
 *
 * Spellskite's redirect on an enters trigger: no "with a single target" restriction, so on a
 * multi-target spell or ability the controller picks which one slot changes, and only to a slot this
 * creature is a legal target for (CR 115.7a); otherwise the targets stay unchanged.
 */
val MizziumMeddler = card("Mizzium Meddler") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Vedalken Wizard"
    power = 1
    toughness = 4
    oracleText = "Flash (You may cast this spell any time you could cast an instant.)\n" +
        "When this creature enters, you may change a target of target spell or ability to this creature."

    keywords(Keyword.FLASH)

    triggeredAbility {
        target(TargetFilter.SpellOrAbilityOnStack)
        trigger = Triggers.self.enters()
        optional = true
        effect = Effects.ChangeTarget(to = EffectTarget.Self)
        description = "When this creature enters, you may change a target of target spell or ability to this creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "64"
        artist = "Johann Bodin"
        flavorText = "Energy surges in Izzet labs are opportunities for experimentation."
        imageUri = "https://cards.scryfall.io/normal/front/7/6/762d568d-a2d6-449b-85c2-a92755efe4c3.jpg?1783938350"
        ruling("2015-06-22", "If changing one target of a spell or ability to Mizzium Meddler would make other targets of that spell or ability illegal, the targets remain unchanged.")
        ruling("2015-06-22", "If the spell or ability has multiple instances of the word \"target,\" you choose which target you're changing to Mizzium Meddler as Mizzium Meddler's ability resolves.")
        ruling("2015-06-22", "The target of the spell or ability won't change unless Mizzium Meddler fulfills all the targeting criteria. If a spell or ability has multiple targets, such as Send to Sleep, you can change only one of the targets to Mizzium Meddler.")
        ruling("2015-06-22", "If a spell or ability has a variable number of targets, you can't change the number of targets.")
        ruling("2015-06-22", "If Mizzium Meddler leaves the battlefield before its ability resolves, the targets remain unchanged.")
        ruling("2015-06-22", "Mizzium Meddler's triggered ability can target a spell or ability even if Mizzium Meddler wouldn't be a legal target for that spell or ability. However, the target of that spell or ability will remain unchanged.")
        ruling("2015-06-22", "If Mizzium Meddler's triggered ability targets a spell or ability with no targets, nothing happens.")
    }
}
