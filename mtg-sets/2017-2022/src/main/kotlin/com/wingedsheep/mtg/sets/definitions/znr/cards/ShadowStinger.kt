package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Shadow Stinger — Zendikar Rising #123
 * {2}{B} · Creature — Vampire Rogue · 1/4
 *
 * Tap another untapped Rogue you control: This creature gains deathtouch until end of turn.
 * Whenever this creature deals combat damage to a player, that player mills three cards.
 *
 * The cost is [Costs.TapAnotherPermanent] — "another" excludes the Stinger itself, and with no {T}
 * symbol neither creature is bound by summoning sickness (per the ruling).
 */
val ShadowStinger = card("Shadow Stinger") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Rogue"
    oracleText = "Tap another untapped Rogue you control: This creature gains deathtouch until end of turn.\n" +
        "Whenever this creature deals combat damage to a player, that player mills three cards. " +
        "(They put the top three cards of their library into their graveyard.)"
    power = 1
    toughness = 4

    activatedAbility {
        cost = Costs.TapAnotherPermanent(GameObjectFilter.Permanent.withSubtype("Rogue"))
        effect = Effects.GrantKeyword(Keyword.DEATHTOUCH, EffectTarget.Self)
        description = "Tap another untapped Rogue you control: This creature gains deathtouch until end of turn."
    }

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Patterns.Library.mill(3, EffectTarget.PlayerRef(Player.TriggeringPlayer))
        description = "Whenever this creature deals combat damage to a player, that player mills three cards."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "123"
        artist = "John Thacker"
        imageUri = "https://cards.scryfall.io/normal/front/0/9/093e0b1e-2cd9-4743-9c0e-b8f3fbc41798.jpg?1783929367"
        ruling(
            "2020-09-25",
            "You can tap any other untapped Rogue you control, including one you haven't controlled " +
                "continuously since the beginning of your most recent turn, to pay the cost of Shadow " +
                "Stinger's first ability."
        )
    }
}
