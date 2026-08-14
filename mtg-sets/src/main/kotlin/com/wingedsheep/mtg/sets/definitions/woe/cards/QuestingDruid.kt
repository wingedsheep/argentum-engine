package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Questing Druid // Seek the Beast
 * {1}{G}
 * Creature — Human Druid
 * 1/1
 * Whenever you cast a spell that's white, blue, black, or red, put a +1/+1 counter on this creature.
 *
 * Adventure: Seek the Beast — {1}{R}, Instant — Adventure
 * Exile the top two cards of your library. Until your next end step, you may play those cards.
 *
 * The trigger is a single [CardPredicate.Or] over four [CardPredicate.HasColor]s rather than four
 * separate triggers: a spell that is more than one of the listed colors still matches the one
 * predicate once, which is exactly the ruling ("Questing Druid's ability still triggers only
 * once"). Green-only and colorless spells match nothing, so casting the Druid itself does not
 * grow it.
 *
 * The Adventure is plain impulse draw — [Patterns.Exile.impulse] with a two-card count and a
 * [MayPlayExpiry.UntilNextEndStep] window ("until your next end step"; on your own turn this
 * turn's end step counts). The permission grants *playing*, not free casting, so the exiled cards
 * still cost their mana and a land among them still uses your land drop at sorcery speed.
 *
 * (CR 715: Adventure cards. Casting the Adventure exiles the card on resolution and lets the
 * caster cast it as the creature spell while it remains in exile.)
 */
val QuestingDruid = card("Questing Druid") {
    manaCost = "{1}{G}"
    colorIdentity = "GR"
    typeLine = "Creature — Human Druid"
    oracleText = "Whenever you cast a spell that's white, blue, black, or red, put a +1/+1 counter " +
        "on this creature."
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = Triggers.youCastSpell(
            GameObjectFilter.Any.withCardPredicate(
                CardPredicate.Or(
                    listOf(
                        CardPredicate.HasColor(Color.WHITE),
                        CardPredicate.HasColor(Color.BLUE),
                        CardPredicate.HasColor(Color.BLACK),
                        CardPredicate.HasColor(Color.RED),
                    ),
                ),
            ),
        )
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    adventure("Seek the Beast") {
        manaCost = "{1}{R}"
        typeLine = "Instant — Adventure"
        oracleText = "Exile the top two cards of your library. Until your next end step, you may " +
            "play those cards. (Then exile this card. You may cast the creature later from exile.)"

        spell {
            effect = Patterns.Exile.impulse(
                count = 2,
                expiry = MayPlayExpiry.UntilNextEndStep,
                storeAs = "seek_the_beast_exiled",
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "234"
        artist = "Jason A. Engle"
        imageUri = "https://cards.scryfall.io/normal/front/7/2/72c130e2-1e17-4996-a5ae-231155d68261.jpg?1783915063"

        ruling(
            "2023-09-01",
            "If you cast a spell that's more than one of the listed colors, Questing Druid's " +
                "ability still triggers only once."
        )
        ruling(
            "2023-09-01",
            "You pay all costs and follow all normal timing rules for cards played from exile with " +
                "Seek the Beast. For example, if the exiled card is a land card, you may play it " +
                "only during your main phase while the stack is empty."
        )
    }
}
