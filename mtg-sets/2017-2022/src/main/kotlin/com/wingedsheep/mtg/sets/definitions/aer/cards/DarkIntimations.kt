package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dark Intimations
 * {2}{U}{B}{R}
 * Sorcery
 *
 * Each opponent sacrifices a creature or planeswalker of their choice, then discards a card. You
 * return a creature or planeswalker card from your graveyard to your hand, then draw a card.
 * When you cast a Bolas planeswalker spell, exile this card from your graveyard. That planeswalker
 * enters with an additional loyalty counter on it.
 *
 * The return is not targeted: the card is chosen from your graveyard as the spell resolves (so
 * Dark Intimations, still on the stack, can't pick itself), and the draw happens even when there
 * is nothing to return.
 *
 * The graveyard trigger (`triggerZone = Zone.GRAVEYARD`) puts the loyalty counter on the
 * planeswalker spell on the stack (`EffectTarget.TriggeringEntity`), the Kumano Faces Kakkazan
 * shape: the counter travels with the spell and is added to its starting loyalty as it enters.
 * The counter isn't gated on the exile ("exile … . That planeswalker enters …", no "if you do"),
 * and two copies in the graveyard each trigger, giving two extra counters.
 */
val DarkIntimations = card("Dark Intimations") {
    manaCost = "{2}{U}{B}{R}"
    colorIdentity = "UBR"
    typeLine = "Sorcery"
    oracleText = "Each opponent sacrifices a creature or planeswalker of their choice, then discards a card. " +
        "You return a creature or planeswalker card from your graveyard to your hand, then draw a card.\n" +
        "When you cast a Bolas planeswalker spell, exile this card from your graveyard. That planeswalker " +
        "enters with an additional loyalty counter on it."

    spell {
        effect = Effects.Sacrifice(
            GameObjectFilter.CreatureOrPlaneswalker,
            target = EffectTarget.PlayerRef(Player.EachOpponent)
        ) then Effects.EachOpponentDiscards(1) then Effects.Pipeline {
            val pool = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.CreatureOrPlaneswalker))
            val returned = chooseExactly(
                1,
                from = pool,
                prompt = "Return a creature or planeswalker card from your graveyard to your hand",
                selectedLabel = "Return to hand"
            )
            toHand(returned)
        } then Effects.DrawCards(1)
    }

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Planeswalker.withSubtype("Bolas"))
        triggerZone = Zone.GRAVEYARD
        effect = Effects.Move(EffectTarget.Self, Zone.EXILE, fromZone = Zone.GRAVEYARD) then
            Effects.AddCounters(CounterType.LOYALTY, 1, EffectTarget.TriggeringEntity)
        description = "When you cast a Bolas planeswalker spell, exile this card from your graveyard. " +
            "That planeswalker enters with an additional loyalty counter on it."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "128"
        artist = "Chase Stone"
        imageUri = "https://cards.scryfall.io/normal/front/d/d/dd8f103c-a688-4f22-ae59-2bce1db9d44a.jpg?1783936737"
        ruling(
            "2017-02-09",
            "Each opponent chooses a permanent to sacrifice from among the creatures and planeswalkers " +
                "that player controls. You don't choose which type of permanent the player has to sacrifice."
        )
        ruling(
            "2017-02-09",
            "If an opponent can't sacrifice a creature or planeswalker, that player still discards a card " +
                "if able. You still return a creature or planeswalker card to your hand if able, even if no " +
                "opponent sacrifices a permanent and/or discards a card. You still draw a card even if you " +
                "can't return a card to your hand."
        )
        ruling(
            "2017-02-09",
            "If two or more Dark Intimations are in your graveyard when you cast a Bolas planeswalker spell, " +
                "you'll exile each of them and that planeswalker will enter the battlefield with that many " +
                "additional loyalty counters."
        )
        ruling(
            "2017-02-09",
            "You choose the creature or planeswalker card to return to your hand while Dark Intimations is " +
                "resolving. No player may take actions between the time you choose one and the time you put " +
                "it into your hand."
        )
    }
}
