package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Azor, the Lawbringer
 * {2}{W}{W}{U}{U}
 * Legendary Creature — Sphinx
 * 6/6
 * Flying
 * When Azor enters, each opponent can't cast instant or sorcery spells during that player's next turn.
 * Whenever Azor attacks, you may pay {X}{W}{U}{U}. If you do, you gain X life and draw X cards.
 *
 * The enters trigger is Sphinx's Decree ([Duration.DuringAffectedPlayersNextTurn]); the attack
 * trigger pays X and {W}{U}{U} in one `Effects.MayPayX(additionalManaCost = …)` payment.
 */
val AzorTheLawbringer = card("Azor, the Lawbringer") {
    manaCost = "{2}{W}{W}{U}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Creature — Sphinx"
    oracleText = "Flying\n" +
        "When Azor enters, each opponent can't cast instant or sorcery spells during that player's next turn.\n" +
        "Whenever Azor attacks, you may pay {X}{W}{U}{U}. If you do, you gain X life and draw X cards."
    power = 6
    toughness = 6

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CantCastSpells(
            EffectTarget.PlayerRef(Player.EachOpponent),
            Duration.DuringAffectedPlayersNextTurn,
            GameObjectFilter.InstantOrSorcery
        )
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.MayPayX(
            additionalManaCost = ManaCost.parse("{W}{U}{U}"),
            descriptionOverride = "You may pay {X}{W}{U}{U}. If you do, you gain X life and draw X cards.",
            then = Effects.GainLife(DynamicAmounts.xValue()) then Effects.DrawCards(DynamicAmounts.xValue()),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "154"
        artist = "Ryan Pancoast"
        imageUri = "https://cards.scryfall.io/normal/front/3/0/30dc237e-b28a-4b65-9790-6b434828bf2e.jpg?1783935277"
    }
}
