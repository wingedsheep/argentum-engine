package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val GhituAmplifier = card("Ghitu Amplifier") {
    manaCost = "{1}{R}"
    colorIdentity = "UR"
    typeLine = "Creature — Human Wizard"
    power = 1
    toughness = 2
    oracleText = "Kicker {2}{U} (You may pay an additional {2}{U} as you cast this spell.)\n" +
        "When this creature enters, if it was kicked, return target creature an opponent controls to its owner's hand.\n" +
        "Whenever you cast an instant or sorcery spell, this creature gets +2/+0 until end of turn."

    keywordAbility(KeywordAbility.kicker("{2}{U}"))

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.WasKicked
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.ReturnToHand(creature)
    }

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.InstantOrSorcery)
        effect = Effects.ModifyStats(2, 0, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "127"
        artist = "Deruchenko Alexander"
        imageUri = "https://cards.scryfall.io/normal/front/b/1/b1845806-7585-40d5-9d3a-f13667e6a9d2.jpg?1783921318"
    }
}
