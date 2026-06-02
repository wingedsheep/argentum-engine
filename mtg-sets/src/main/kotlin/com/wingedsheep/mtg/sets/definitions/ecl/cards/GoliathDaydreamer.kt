package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.effects.ConditionalOnCollectionEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect
import com.wingedsheep.sdk.scripting.effects.GrantPlayWithoutPayingCostEffect
import com.wingedsheep.sdk.scripting.effects.MarkSpellExileWithCountersEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.Effects

/**
 * Goliath Daydreamer
 * {2}{R}{R}
 * Creature — Giant Wizard
 * 4/4
 *
 * Whenever you cast an instant or sorcery spell from your hand, exile that card
 * with a dream counter on it instead of putting it into your graveyard as it
 * resolves.
 *
 * Whenever this creature attacks, you may cast a spell from among cards you own
 * in exile with dream counters on them without paying its mana cost.
 */
val GoliathDaydreamer = card("Goliath Daydreamer") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Giant Wizard"
    power = 4
    toughness = 4
    oracleText = "Whenever you cast an instant or sorcery spell from your hand, exile that card " +
        "with a dream counter on it instead of putting it into your graveyard as it resolves.\n" +
        "Whenever this creature attacks, you may cast a spell from among cards you own in exile " +
        "with dream counters on them without paying its mana cost."

    // First ability — re-route resolution to exile with a dream counter.
    triggeredAbility {
        trigger = Triggers.youCastSpell(
            spellFilter = GameObjectFilter.InstantOrSorcery,
            requires = setOf(SpellCastPredicate.CastFromZone(Zone.HAND)),
        )
        effect = MarkSpellExileWithCountersEffect(
            target = EffectTarget.TriggeringEntity,
            counterType = Counters.DREAM,
            count = 1
        )
    }

    // Attack trigger — may cast a card you own in exile with a dream counter for free.
    // Granting MayPlayFromExile + PlayWithoutPayingCost on the chosen card is the standard
    // free-cast permission pattern. Per ruling, the cast should occur during this ability's
    // resolution; the EndOfTurn permission is the closest practical match the engine provides.
    triggeredAbility {
        trigger = Triggers.Attacks
        optional = true
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.FromZone(
                        zone = Zone.EXILE,
                        player = Player.You,
                        filter = GameObjectFilter.Any.withCounter(Counters.DREAM)
                    ),
                    storeAs = "dreamPool"
                ),
                ConditionalOnCollectionEffect(
                    collection = "dreamPool",
                    ifNotEmpty = Effects.Composite(
                        listOf(
                            SelectFromCollectionEffect(
                                from = "dreamPool",
                                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                                storeSelected = "toCast"
                            ),
                            GrantMayPlayFromExileEffect(from = "toCast"),
                            GrantPlayWithoutPayingCostEffect(from = "toCast")
                        )
                    )
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "143"
        artist = "Omar Rayyan"
        imageUri = "https://cards.scryfall.io/normal/front/8/8/88e8cd13-2a29-4df6-937c-1bed68fbeafa.jpg?1767658300"

        ruling("2025-11-17", "Goliath Daydreamer's last ability allows you to cast an exiled card with a dream counter on it only while the ability is resolving and still on the stack. You can't wait to cast the spell later in the turn. Timing restrictions based on the card's type are ignored.")
        ruling("2025-11-17", "The spell will have all of its normal effects before being exiled. If the spell's normal effects include exiling itself, the effect of Goliath Daydreamer's first ability won't apply to it.")
        ruling("2025-11-17", "If the spell you cast has {X} in its mana cost, you must choose 0 as the value of X when casting it without paying its mana cost.")
        ruling("2025-11-17", "If a spell is countered or otherwise fails to resolve, Goliath Daydreamer's first ability won't exile it.")
        ruling("2025-11-17", "Since you are using an alternative cost to cast the spell, you can't pay any other alternative costs. You can, however, pay additional costs, such as kicker costs. If the card has any mandatory additional costs, you must pay those.")
    }
}
