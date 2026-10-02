package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Unstable Amulet {1}{R}
 * Artifact
 *
 * When this artifact enters, you get {E}{E} (two energy counters).
 * Whenever you cast a spell from anywhere other than your hand, this artifact deals 1 damage to
 * each opponent.
 * {T}, Pay {E}{E}: Exile the top card of your library. You may play it until you exile another
 * card with this artifact.
 *
 * The impulse grant uses [MayPlayExpiry.UntilSourceExilesAnother] (cf. Superior Foes of
 * Spider-Man): it survives the Amulet leaving the battlefield and is revoked only when this same
 * Amulet exiles another card. Cards played this way are cast from exile, so they feed the
 * second ability.
 */
val UnstableAmulet = card("Unstable Amulet") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Artifact"
    oracleText = "When this artifact enters, you get {E}{E} (two energy counters).\n" +
        "Whenever you cast a spell from anywhere other than your hand, this artifact deals 1 damage to each opponent.\n" +
        "{T}, Pay {E}{E}: Exile the top card of your library. You may play it until you exile another card with this artifact."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GetEnergy(2)
    }

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.CastFromZoneOtherThan(Zone.HAND)))
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.PayPlayerCounters(CounterType.ENERGY, 2))
        effect = Patterns.Exile.impulse(count = 1, expiry = MayPlayExpiry.UntilSourceExilesAnother)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "142"
        artist = "José Parodi"
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9d9949f5-8d6c-4ea9-b203-99e8a57a6c60.jpg?1783911264"

        ruling(
            "2024-06-07",
            "Unstable Amulet's second ability counts any spells cast from zones other than your hand. " +
                "These are usually spells cast from exile, the graveyard, or the command zone. It also " +
                "counts spells cast from outside the game, such as spells cast with Wish or Garth One-Eye's ability."
        )
        ruling(
            "2024-06-07",
            "You pay all costs and follow all normal timing rules for cards played with Unstable Amulet's " +
                "last ability. For example, if the exiled card is a land card, you may play it only during " +
                "your main phase while the stack is empty."
        )
        ruling(
            "2024-06-07",
            "If Unstable Amulet leaves the battlefield before you play the most recently exiled card, you " +
                "can play that card for as long as it remains exiled."
        )
    }
}
