package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mana Vault — Limited Edition Alpha #259
 * {1} · Artifact
 *
 * This artifact doesn't untap during your untap step.
 * At the beginning of your upkeep, you may pay {4}. If you do, untap this artifact.
 * At the beginning of your draw step, if this artifact is tapped, it deals 1 damage to you.
 * {T}: Add {C}{C}{C}.
 *
 * The Goblin Dirigible upkeep buy-back ([AbilityFlag.DOESNT_UNTAP] + [Effects.MayPay] →
 * [Effects.Untap] on [EffectTarget.Self]), Basalt Monolith's mana ability, and a draw-step trigger
 * with Dwarven Hold's intervening "if" (`Conditions.SourceIsTapped`, CR 603.4) — untapping it in
 * response stops the damage. The Vault itself is the damage source.
 */
val ManaVault = card("Mana Vault") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "This artifact doesn't untap during your untap step.\n" +
        "At the beginning of your upkeep, you may pay {4}. If you do, untap this artifact.\n" +
        "At the beginning of your draw step, if this artifact is tapped, it deals 1 damage to you.\n" +
        "{T}: Add {C}{C}{C}."

    flags(AbilityFlag.DOESNT_UNTAP)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.MayPay(ManaCost.parse("{4}"), Effects.Untap(EffectTarget.Self))
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.DRAW)
        interveningIf = Conditions.SourceIsTapped
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.You))
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(3)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "259"
        artist = "Mark Tedin"
        imageUri = "https://cards.scryfall.io/normal/front/1/9/19499cb7-eccb-4e69-af32-6002d447a160.jpg?1783948664"
    }
}
