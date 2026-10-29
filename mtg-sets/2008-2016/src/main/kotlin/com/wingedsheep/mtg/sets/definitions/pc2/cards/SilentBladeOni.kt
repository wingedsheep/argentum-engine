package com.wingedsheep.mtg.sets.definitions.pc2.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.ninjutsu
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Silent-Blade Oni — Planechase 2012 #105 (canonical printing)
 * {3}{U}{U}{B}{B} · Creature — Demon Ninja · 6/5
 *
 * Ninjutsu {4}{U}{B}
 * Whenever this creature deals combat damage to a player, look at that player's hand. You may cast
 * a spell from among those cards without paying its mana cost.
 *
 * Mindleech Mass's pipeline without its "you may look": the look is mandatory, the cast is an
 * up-to-one choice among the damaged player's nonland cards, cast during resolution.
 */
val SilentBladeOni = card("Silent-Blade Oni") {
    manaCost = "{3}{U}{U}{B}{B}"
    colorIdentity = "UB"
    typeLine = "Creature — Demon Ninja"
    power = 6
    toughness = 5
    oracleText = "Ninjutsu {4}{U}{B} ({4}{U}{B}, Return an unblocked attacker you control to hand: Put " +
        "this card onto the battlefield from your hand tapped and attacking.)\n" +
        "Whenever this creature deals combat damage to a player, look at that player's hand. You may " +
        "cast a spell from among those cards without paying its mana cost."

    ninjutsu("{4}{U}{B}")

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.Pipeline {
            run(Effects.LookAtHand(EffectTarget.PlayerRef(Player.TriggeringPlayer)))
            val theirHand = gather(CardSource.FromZone(Zone.HAND, Player.TriggeringPlayer))
            val spellToCast = chooseUpTo(
                1,
                from = theirHand,
                filter = GameObjectFilter.Nonland,
                showAllCards = true,
                prompt = "You may cast a spell from their hand without paying its mana cost",
                selectedLabel = "Cast for free"
            )
            run(Effects.CastFromCollectionWithoutPayingCost(spellToCast))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "105"
        artist = "Steve Prescott"
        imageUri = "https://cards.scryfall.io/normal/front/6/3/63fb057c-a70f-49e4-ba39-f5ce17b09c7f.jpg?1783940593"
        ruling("2012-06-01", "If you cast a permanent spell this way, it will enter under your control when it resolves. If you cast an instant or sorcery spell this way, that card will be put into its owner's graveyard when it resolves.")
        ruling("2012-06-01", "The nonland card you cast via the triggered ability is cast as part of the resolution of that ability. Timing restrictions based on the card's type are ignored. Other restrictions, such as \"Cast [this card] only during an opponent's turn,\" are not.")
        ruling("2012-06-01", "If you cast a card without paying its mana cost, you can't choose to cast it for any alternative costs, such as evoke or the alternative cost provided by the morph ability. If it has {X} in its mana cost, X must be 0. However, you can pay optional additional costs, such as kicker, and you must still pay mandatory additional costs, such as the one on Fling.")
    }
}
