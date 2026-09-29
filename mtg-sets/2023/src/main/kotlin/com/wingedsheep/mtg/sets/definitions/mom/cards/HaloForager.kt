package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.AfterResolveDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Halo Forager — March of the Machine #227
 * {1}{U}{B} · Creature — Faerie Rogue · 3/1
 *
 * Flying
 * When this creature enters, you may pay {X}. When you do, you may cast target instant or sorcery
 * card with mana value X from a graveyard without paying its mana cost. If that spell would be put
 * into a graveyard, exile it instead.
 *
 * The ETB trigger has no target; [Effects.MayPayX] asks for X as it resolves and pays it. Paying
 * fires a reflexive trigger (CR 603.12) whose target — an instant or sorcery card in *any*
 * graveyard with mana value X — is chosen as it goes on the stack: the paid X rides on the
 * reflexive trigger's context, which is what `manaValueEqualsX()` reads at targeting and at the
 * resolution re-check. On resolution the controller may cast it right then (Wishing Well's shape),
 * with the exile rider stamped on the spell as it's cast.
 *
 * Fidelity note: the engine's X chooser treats X = 0 as declining the payment, so the legal
 * "pay {0}, cast a mana value 0 instant or sorcery" line is not offered.
 */
val HaloForager = card("Halo Forager") {
    manaCost = "{1}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Creature — Faerie Rogue"
    power = 3
    toughness = 1
    oracleText = "Flying\n" +
        "When this creature enters, you may pay {X}. When you do, you may cast target instant or " +
        "sorcery card with mana value X from a graveyard without paying its mana cost. If that " +
        "spell would be put into a graveyard, exile it instead."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.MayPayX(
            then = Effects.ReflexiveTrigger(
                action = Effects.Nothing,
                optional = false,
                reflexiveTargetRequirements = listOf(
                    TargetObject(
                        filter = TargetFilter(
                            GameObjectFilter.InstantOrSorcery.manaValueEqualsX(),
                            zone = Zone.GRAVEYARD
                        )
                    )
                ),
                reflexiveEffect = Effects.Pipeline {
                    val foraged = gather(CardSource.ChosenTargets)
                    run(Effects.May(
                        Effects.CastFromCollectionWithoutPayingCost(
                            from = foraged,
                            insteadOfGraveyard = AfterResolveDestination.EXILE
                        )
                    ))
                },
                descriptionOverride = "you may cast target instant or sorcery card with mana value X " +
                    "from a graveyard without paying its mana cost. If that spell would be put into a " +
                    "graveyard, exile it instead."
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "227"
        artist = "Kevin Sidharta"
        imageUri = "https://cards.scryfall.io/normal/front/e/d/edb5f0cc-c826-4e7b-882c-63f6e51fa932.jpg?1783916950"
        ruling("2023-04-14", "Halo Forager's enters-the-battlefield ability triggers and goes on the stack without a target. If you pay {X}, the second \"reflexive\" triggered ability will trigger. You'll choose the target instant or sorcery card for that second ability at that time, based on the value you chose for X. X can be 0.")
        ruling("2023-04-14", "You choose whether or not to cast the target instant or sorcery card as the reflexive triggered ability resolves. If you do, you do so as part of the resolution of the triggered ability. You can't wait to cast it later in the turn. Timing restrictions based on the card's type are ignored.")
        ruling("2023-04-14", "If you cast a card \"without paying its mana cost,\" you can't pay any alternative costs. You can, however, pay additional costs. If the card has any mandatory additional costs, those must be paid to cast the card.")
        ruling("2023-04-14", "If the card has {X} in its mana cost, you must choose 0 as the value of X when casting it without paying its mana cost.")
    }
}
