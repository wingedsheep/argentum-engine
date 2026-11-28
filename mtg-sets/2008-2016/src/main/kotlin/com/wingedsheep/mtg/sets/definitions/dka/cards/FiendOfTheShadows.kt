package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Fiend of the Shadows
 * {3}{B}{B}
 * Creature — Vampire Wizard
 * 3/3
 * Flying
 * Whenever this creature deals combat damage to a player, that player exiles a card from their
 * hand. You may play that card for as long as it remains exiled.
 * Sacrifice a Human: Regenerate this creature.
 *
 * The damaged player ([Chooser.TriggeringPlayer]) picks the card from their own hand; the play
 * permission then goes to the Fiend's controller, permanently — it survives the Fiend leaving the
 * battlefield or changing controllers. "Play" covers lands too, so the grant isn't nonland-only.
 */
val FiendOfTheShadows = card("Fiend of the Shadows") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Wizard"
    power = 3
    toughness = 3
    oracleText = "Flying\n" +
        "Whenever this creature deals combat damage to a player, that player exiles a card from their " +
        "hand. You may play that card for as long as it remains exiled.\n" +
        "Sacrifice a Human: Regenerate this creature."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.Pipeline {
            val hand = gather(CardSource.FromZone(Zone.HAND, Player.TriggeringPlayer))
            val exiled = chooseExactly(
                1,
                from = hand,
                chooser = Chooser.TriggeringPlayer,
                prompt = "Choose a card to exile from your hand — your opponent may play it"
            )
            exile(exiled, owner = Player.TriggeringPlayer)
            run(Effects.GrantMayPlayFromExile(from = exiled, expiry = MayPlayExpiry.Permanent))
        }
    }

    activatedAbility {
        cost = Costs.Sacrifice(GameObjectFilter.Permanent.withSubtype(Subtype.HUMAN))
        effect = Effects.Regenerate(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "62"
        artist = "Igor Kieryluk"
        imageUri = "https://cards.scryfall.io/normal/front/3/8/38167118-f5b3-4e07-8060-b170b49cff9e.jpg?1783940830"
        ruling("2011-01-22", "Playing a card exiled with Fiend of the Shadows follows all the normal rules for playing that card. You must pay its costs, and you must follow all timing restrictions, for example.")
        ruling("2011-01-22", "You may only play the card, meaning play it as a land or cast it as a spell. It's not in your hand. You can't discard it, activate its cycling abilities, and so on.")
        ruling("2011-01-22", "As long as the card remains exiled, you may still play it, even if Fiend of the Shadows leaves the battlefield or changes controllers.")
    }
}
