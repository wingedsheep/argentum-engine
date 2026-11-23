package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.AttackPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Nav Squad Commandos
 * {4}{W}
 * Creature — Human Soldier
 * 3/5
 * Battalion — Whenever this creature and at least two other creatures attack, this creature gets
 * +1/+1 until end of turn. Untap it.
 */
val NavSquadCommandos = card("Nav Squad Commandos") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 3
    toughness = 5
    oracleText = "Battalion — Whenever this creature and at least two other creatures attack, this creature " +
        "gets +1/+1 until end of turn. Untap it."

    triggeredAbility {
        trigger = Triggers.self.attacks(setOf(AttackPredicate.AttackerCountAtLeast(3)))
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self) then Effects.Untap(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "22"
        artist = "Steve Prescott"
        flavorText = "Funded by the Azorius to keep current maps of the undercity, the Boros nav squads survey even the most unsavory areas."
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9d81d7f8-375f-40f5-98cd-08be08580bef.jpg?1783940140"
    }
}
