package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The Kami War // O-Kagachi Made Manifest
 * {1}{W}{U}{B}{R}{G} — Enchantment — Saga // Enchantment Creature — Dragon Spirit 6/6
 *
 * Front — The Kami War:
 *   I — Exile target nonland permanent an opponent controls.
 *   II — Return up to one other target nonland permanent to its owner's hand. Then each opponent
 *        discards a card.
 *   III — Exile this Saga, then return it to the battlefield transformed under your control.
 *
 * Back — O-Kagachi Made Manifest:
 *   O-Kagachi Made Manifest is all colors.
 *   Flying, trample
 *   Whenever this creature attacks, defending player chooses a nonland card in your graveyard.
 *   Return that card to your hand. This creature gets +X/+0 until end of turn, where X is the mana
 *   value of that card.
 *
 * "Is all colors" is a characteristic-defining ability on a face with no mana cost, carried by the
 * face's colour indicator (as on Awaken the Maelstrom). The attack trigger is not targeted: the
 * defending player picks on resolution, and X reads the chosen card's mana value after it has moved
 * to your hand — with no nonland card in your graveyard, nothing returns and X is 0.
 */
private val OKagachiMadeManifest = card("O-Kagachi Made Manifest") {
    manaCost = ""
    colorIndicator = "WUBRG"
    colorIdentity = "WUBRG"
    typeLine = "Enchantment Creature — Dragon Spirit"
    oracleText = "O-Kagachi Made Manifest is all colors.\n" +
        "Flying, trample\n" +
        "Whenever this creature attacks, defending player chooses a nonland card in your graveyard. " +
        "Return that card to your hand. This creature gets +X/+0 until end of turn, where X is the " +
        "mana value of that card."
    power = 6
    toughness = 6

    keywords(Keyword.FLYING, Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.Pipeline {
            val graveyard = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.You))
            val chosen = chooseExactly(
                1,
                from = graveyard,
                chooser = Chooser.DefendingPlayer,
                filter = GameObjectFilter.Nonland,
                prompt = "Choose a nonland card for the attacking player to return to their hand",
            )
            toHand(chosen)
            run(
                Effects.ModifyStats(
                    power = DynamicAmounts.manaValueOf(chosen),
                    toughness = DynamicAmounts.fixed(0),
                    target = EffectTarget.Self,
                )
            )
        }
        description = "Whenever this creature attacks, defending player chooses a nonland card in your " +
            "graveyard. Return that card to your hand. This creature gets +X/+0 until end of turn, where " +
            "X is the mana value of that card."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "227"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/back/3/6/36052532-5028-43a8-9fc4-56221ec867fd.jpg?1783923839"
    }
}

private val TheKamiWarFront = card("The Kami War") {
    manaCost = "{1}{W}{U}{B}{R}{G}"
    colorIdentity = "WUBRG"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter.)\n" +
        "I — Exile target nonland permanent an opponent controls.\n" +
        "II — Return up to one other target nonland permanent to its owner's hand. Then each opponent " +
        "discards a card.\n" +
        "III — Exile this Saga, then return it to the battlefield transformed under your control."

    sagaChapter(1) {
        val permanent = target(TargetFilter.NonlandPermanentOpponentControls)
        effect = Effects.Exile(permanent)
    }

    sagaChapter(2) {
        val permanent = target(TargetFilter.NonlandPermanent.other(), optional = true)
        effect = Effects.ReturnToHand(permanent) then Effects.EachOpponentDiscards(1)
    }

    sagaChapter(3) {
        effect = Effects.ExileAndReturnTransformed()
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "227"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/3/6/36052532-5028-43a8-9fc4-56221ec867fd.jpg?1783923839"
    }
}

val TheKamiWar: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = TheKamiWarFront,
    backFace = OKagachiMadeManifest,
)
