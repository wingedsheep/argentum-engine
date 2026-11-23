package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.AttackPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Daring Skyjek
 * {1}{W}
 * Creature — Human Knight
 * 3/1
 * Battalion — Whenever this creature and at least two other creatures attack, this creature gains
 * flying until end of turn.
 */
val DaringSkyjek = card("Daring Skyjek") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Knight"
    power = 3
    toughness = 1
    oracleText = "Battalion — Whenever this creature and at least two other creatures attack, this creature " +
        "gains flying until end of turn."

    triggeredAbility {
        trigger = Triggers.self.attacks(setOf(AttackPredicate.AttackerCountAtLeast(3)))
        effect = Effects.GrantKeyword(Keyword.FLYING, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "9"
        artist = "Jason Chan"
        flavorText = "\"The hard part isn't landing in the saddle. The hard part is leaping before you see it.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6c28412d-9add-4911-8487-c84559006fb0.jpg?1783940145"
    }
}
