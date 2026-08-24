package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Pang Tong, "Young Phoenix"
 * {1}{W}{W}
 * Legendary Creature — Human Advisor
 * 1/2
 *
 * The Portal "combat trick on a stick" shape (Stern Marshal's cycle): a tap ability whose timing
 * clause is two [ActivationRestriction]s — [ActivationRestriction.OnlyDuringYourTurn] plus
 * [ActivationRestriction.BeforeStep] on the declare-attackers step.
 */
val PangTongYoungPhoenix = card("Pang Tong, \"Young Phoenix\"") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Advisor"
    power = 1
    toughness = 2
    oracleText = "{T}: Target creature gets +0/+2 until end of turn. Activate only during your turn, before attackers are declared."

    activatedAbility {
        cost = AbilityCost.Tap
        restrictions = listOf(
            ActivationRestriction.OnlyDuringYourTurn,
            ActivationRestriction.BeforeStep(Step.DECLARE_ATTACKERS)
        )
        val t = target("target", TargetCreature(filter = TargetFilter.Creature))
        effect = Effects.ModifyStats(0, 2, t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "14"
        artist = "Li Tie"
        flavorText = "\". . . It was Pang Tong's boat-connecting scheme That let Zhou Yu accomplish his great deed.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f484d47a-fb1d-4746-8f1d-dd9d24e67c1a.jpg"
    }
}
