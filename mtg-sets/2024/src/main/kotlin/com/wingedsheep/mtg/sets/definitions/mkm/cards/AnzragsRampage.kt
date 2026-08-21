package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.ForEachInCollectionEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Anzrag's Rampage — Murders at Karlov Manor #111
 * {3}{R}{R} · Sorcery
 *
 * Destroy all artifacts you don't control, then exile the top X cards of your library, where X is
 * the number of artifacts that were put into graveyards from the battlefield this turn. You may put
 * a creature card exiled this way onto the battlefield. It gains haste. Return it to your hand at
 * the beginning of the next end step.
 *
 * **"then" is load-bearing, and one pipeline is what buys it.** The artifacts this spell destroys
 * are themselves artifacts put into graveyards from the battlefield this turn, so they count toward
 * X. Pipeline steps run in order and a `GatherCardsEffect`'s [com.wingedsheep.sdk.scripting.values.DynamicAmount]
 * is evaluated when that step executes, so gathering the top X *after* the destroy sees the
 * freshly-incremented tally for free. Splitting this into two effects would read X as of before the
 * wrath and quietly exile too few cards.
 *
 * X comes from [DynamicAmounts.artifactsDiedThisTurn], which defaults to [com.wingedsheep.sdk.scripting.references.Player.Each]
 * — the tracker is per-player (credited to each artifact's last-known controller) and summing every
 * seat is the game-wide count the card asks for. Artifacts destroyed earlier in the turn by anything
 * else count too, which is the whole point of the card.
 *
 * "You may put a creature card exiled this way onto the battlefield" is `chooseUpTo(1, …)` filtered
 * to creatures: declining leaves the slot empty and every downstream step becomes a silent no-op,
 * which is exactly the "may" semantics. The rest of the exiled cards stay exiled — the card never
 * gives them back.
 *
 * Haste is granted immediately and the return is a delayed trigger, never the other way around:
 * `CreateDelayedTriggerExecutor` bakes a `MoveToZoneEffect`'s target into a `SpecificEntity` at
 * *scheduling* time but has no such case for a keyword grant, so a haste grant deferred inside the
 * delayed trigger would fire against nothing. Both hang off [ForEachInCollectionEffect] over the
 * tracked move rather than a fixed pipeline index, so an empty collection iterates zero times
 * instead of erroring.
 */
val AnzragsRampage = card("Anzrag's Rampage") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Destroy all artifacts you don't control, then exile the top X cards of your " +
        "library, where X is the number of artifacts that were put into graveyards from the " +
        "battlefield this turn. You may put a creature card exiled this way onto the battlefield. " +
        "It gains haste. Return it to your hand at the beginning of the next end step."

    spell {
        effect = Effects.Pipeline {
            // "Destroy all artifacts you don't control," — these land in graveyards before X is
            // read, so they count toward it.
            val theirArtifacts = gather(GameObjectFilter.Artifact.opponentControls(), name = "theirArtifacts")
            destroy(theirArtifacts)

            // "then exile the top X cards of your library, where X is …"
            val exiled = gather(
                CardSource.TopOfLibrary(DynamicAmounts.artifactsDiedThisTurn()),
                name = "exiledThisWay",
            )
            exile(exiled)

            // "You may put a creature card exiled this way onto the battlefield."
            val chosen = chooseUpTo(
                count = 1,
                from = exiled,
                filter = GameObjectFilter.Creature,
                prompt = "You may put a creature card exiled this way onto the battlefield",
                showAllCards = true,
                name = "chosenCreature",
            )
            val entered = moveTracked(
                from = chosen,
                destination = CardDestination.ToZone(Zone.BATTLEFIELD),
                markEnteredViaSourceAbility = true,
                name = "entered",
            )

            // "It gains haste. Return it to your hand at the beginning of the next end step."
            run(
                ForEachInCollectionEffect(
                    collection = entered.key,
                    effect = Effects.Composite(
                        Effects.GrantKeyword(Keyword.HASTE, EffectTarget.Self, Duration.Permanent),
                        CreateDelayedTriggerEffect(
                            step = Step.END,
                            effect = Effects.Move(EffectTarget.Self, Zone.HAND),
                        ),
                    ),
                ),
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "111"
        artist = "Lucas Graciano"
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9dc52b53-3e4f-4d7d-851f-86c6e0ac67b2.jpg?1783912887"
    }
}
