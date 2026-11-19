package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EntersWithCharacteristics

/**
 * Arbiter of the Ideal — Born of the Gods #31
 * {4}{U}{U} · Creature — Sphinx · 4/5
 *
 * Flying
 * Inspired — Whenever this creature becomes untapped, reveal the top card of your library. If
 * it's an artifact, creature, or land card, you may put it onto the battlefield with a
 * manifestation counter on it. That permanent is an enchantment in addition to its other types.
 *
 * The reveal is mandatory; the put is an "up to one" choice over the revealed card when it
 * matches. A card left behind stays on top of the library. The permanent *enters* an
 * enchantment ([EntersWithCharacteristics]), so constellation sees it, and it stays one after
 * the manifestation counter (a memory aid) is removed — both per the rulings.
 */
val ArbiterOfTheIdeal = card("Arbiter of the Ideal") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Sphinx"
    power = 4
    toughness = 5
    oracleText = "Flying\nInspired — Whenever this creature becomes untapped, reveal the top card of your library. " +
        "If it's an artifact, creature, or land card, you may put it onto the battlefield with a manifestation " +
        "counter on it. That permanent is an enchantment in addition to its other types."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.becomesUntapped()
        effect = Effects.Pipeline {
            val top = gather(CardSource.TopOfLibrary(1))
            reveal(top, fromZone = Zone.LIBRARY)
            val chosen = chooseUpTo(
                1,
                from = top,
                filter = GameObjectFilter.Artifact or GameObjectFilter.Creature or GameObjectFilter.Land,
                prompt = "You may put it onto the battlefield with a manifestation counter on it",
                selectedLabel = "Put onto the battlefield",
                remainderLabel = "Leave on top",
                showAllCards = true
            )
            move(
                chosen,
                CardDestination.ToZone(Zone.BATTLEFIELD),
                addCounterType = CounterType.MANIFESTATION,
                entersWith = EntersWithCharacteristics(addedCardTypes = setOf(CardType.ENCHANTMENT))
            )
        }
        description = "Inspired — Whenever this creature becomes untapped, reveal the top card of your library. " +
            "If it's an artifact, creature, or land card, you may put it onto the battlefield with a manifestation " +
            "counter on it. That permanent is an enchantment in addition to its other types."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "31"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/front/3/2/32e58ba3-4202-4f91-b27f-7528d27c1d5c.jpg?1783939573"

        ruling("2014-02-01", "The manifestation counter is a memory aid only. The permanent will continue to be an enchantment in addition to its other types even if that counter is removed.")
        ruling("2014-02-01", "If you choose to not put the card onto the battlefield, or if the card isn't one of the listed types, it will remain on top of your library. (Note that revealing the card is not optional.)")
        ruling("2014-02-01", "The permanent will be an enchantment as it enters the battlefield and will cause constellation abilities of permanents you control to trigger.")
        ruling("2014-02-01", "Inspired abilities trigger no matter how the creature becomes untapped: by the turn-based action at the beginning of the untap step or by a spell or ability.")
        ruling("2014-02-01", "If an inspired ability triggers during your untap step, the ability will be put on the stack at the beginning of your upkeep. If the ability creates one or more token creatures, those creatures won't be able to attack that turn (unless they gain haste).")
        ruling("2014-02-01", "Inspired abilities don't trigger when the creature enters the battlefield.")
    }
}
