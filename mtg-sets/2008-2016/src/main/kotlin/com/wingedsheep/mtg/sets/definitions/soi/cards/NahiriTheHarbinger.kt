package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Nahiri, the Harbinger — Shadows over Innistrad #247
 * {2}{R}{W} · Legendary Planeswalker — Nahiri · Starting loyalty 4
 *
 * +2: You may discard a card. If you do, draw a card.
 * −2: Exile target enchantment, tapped artifact, or tapped creature.
 * −8: Search your library for an artifact or creature card, put it onto the battlefield, then
 *     shuffle. It gains haste. Return it to your hand at the beginning of the next end step.
 *
 * Modeling notes:
 *
 *  - **The +2** is Sarkhan, Fireblood's rummage: a `May` around an `IfYouDo`, so declining (or
 *    holding an empty hand) still adds the loyalty and draws nothing — the first printed ruling.
 *  - **The −2 target** is a *heterogeneous* union: "tapped" binds to the artifact and creature
 *    branches only, never the enchantment. The `or` infix keeps each branch's state predicate
 *    on its own branch (`GameObjectFilter.anyOf`), so an untapped enchantment is legal and an
 *    untapped artifact or creature is not — the Banishing Slash shape.
 *  - **The −8** is a tracked pipeline (Rampaging Growth's search + Anzrag's Rampage's haste and
 *    return): the found card is moved with `moveTracked`, so haste and the delayed return hang
 *    off what actually entered, and an empty search iterates zero times. Haste is granted now and
 *    only the return is deferred. A noncreature artifact still gains haste (printed ruling). The
 *    return is a delayed trigger baked to that object; if it has left the battlefield by the next
 *    end step it is a new object and stays where it is (printed ruling, CR 400.7). Nahiri usually
 *    dies to state-based actions with this on the stack; the ability resolves independently of
 *    its source and the delayed trigger still fires.
 */
val NahiriTheHarbinger = card("Nahiri, the Harbinger") {
    manaCost = "{2}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Planeswalker — Nahiri"
    startingLoyalty = 4
    oracleText = "+2: You may discard a card. If you do, draw a card.\n" +
        "−2: Exile target enchantment, tapped artifact, or tapped creature.\n" +
        "−8: Search your library for an artifact or creature card, put it onto the battlefield, " +
        "then shuffle. It gains haste. Return it to your hand at the beginning of the next end step."

    // +2: You may discard a card. If you do, draw a card.
    loyaltyAbility(+2) {
        effect = Effects.May(
            effect = Effects.IfYouDo(
                action = Patterns.Hand.discardCards(1),
                then = Effects.DrawCards(1),
            ),
            descriptionOverride = "You may discard a card. If you do, draw a card.",
        )
    }

    // −2: Exile target enchantment, tapped artifact, or tapped creature.
    loyaltyAbility(-2) {
        val exiled = target(
            TargetFilter(
                GameObjectFilter.Enchantment or
                    GameObjectFilter.Artifact.tapped() or
                    GameObjectFilter.Creature.tapped()
            )
        )
        effect = Effects.Exile(exiled)
    }

    // −8: Search your library for an artifact or creature card, put it onto the battlefield, then
    //     shuffle. It gains haste. Return it to your hand at the beginning of the next end step.
    loyaltyAbility(-8) {
        effect = Effects.Pipeline {
            val searchable = gather(
                CardSource.FromZone(Zone.LIBRARY, Player.You, GameObjectFilter.CreatureOrArtifact),
                search = true,
            )
            val found = chooseUpTo(
                1,
                from = searchable,
                prompt = "Search your library for an artifact or creature card to put onto the battlefield",
            )
            val entered = moveTracked(found, CardDestination.ToZone(Zone.BATTLEFIELD))
            run(Effects.ShuffleLibrary())
            run(EmitLibrarySearchedEventEffect)
            run(
                Effects.ForEachInCollection(
                    entered,
                    Effects.GrantKeyword(Keyword.HASTE, EffectTarget.IterationEntity, Duration.Permanent) then
                        Effects.CreateDelayedTrigger(
                            step = Step.END,
                            effect = Effects.Move(EffectTarget.IterationEntity, Zone.HAND),
                        ),
                ),
            )
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "247"
        artist = "Aleksi Briclot"
        imageUri = "https://cards.scryfall.io/normal/front/8/4/84d70487-21d1-4897-ac20-2a996e893b5b.jpg?1783937713"
        ruling("2016-04-08", "You can activate Nahiri's first ability with no intention of discarding a card just to add loyalty to Nahiri.")
        ruling("2016-04-08", "If you find a noncreature artifact with Nahiri's third ability, it will still gain haste, even though it won't be meaningful unless that artifact becomes a creature.")
        ruling("2016-04-08", "If the artifact or creature put onto the battlefield with Nahiri's third ability leaves the battlefield before the next end step, it won't be returned to your hand from its new zone.")
    }
}
