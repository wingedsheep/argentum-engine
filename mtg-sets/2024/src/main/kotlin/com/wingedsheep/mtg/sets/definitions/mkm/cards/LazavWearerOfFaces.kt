package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Lazav, Wearer of Faces — Murders at Karlov Manor #216
 * {U}{B} · Legendary Creature — Shapeshifter Detective · 2/3
 *
 * Whenever Lazav attacks, exile target card from a graveyard, then investigate.
 * Whenever you sacrifice a Clue, you may have Lazav become a copy of a creature card exiled with
 * it until end of turn.
 *
 * **"Exiled with it" is Lazav's own linked exile, not the Clue's.** The two abilities are a linked
 * pair (CR 607): the first one does the exiling, the second one refers back to what it exiled, and
 * "it" is the object both abilities are printed on. Reading "it" as the sacrificed Clue would be
 * unsatisfiable — a Clue token never exiles anything, so that pile is always empty. So the attack
 * trigger exiles with `linkToSource = true` (a `LinkedExileComponent` entry on Lazav) and the
 * sacrifice trigger reads the accumulated pile back with [CardSource.FromLinkedExile]. Sacrificing
 * a Clue is the *permission* to copy, not the pool: every card Lazav has ever exiled stays
 * eligible, which is what makes attacking repeatedly worth doing.
 *
 * **The investigate is sequenced after the exile, in one resolution.** Both live in a single
 * `Composite` so the Clue exists only if the ability resolves at all — per the printed ruling, an
 * illegal exile target means the ability doesn't resolve and you don't investigate either.
 *
 * The copy half is Lazav, Familiar Stranger's shape with a different pool: gather → keep the
 * creature cards → choose up to one → become a copy of it until end of turn, reading the copy
 * source from exile via `sourceFromAnyZone`. `chooseUpTo(1)` *is* the printed "you may" —
 * declining selects nothing and the [ConditionalEffect] gate leaves Lazav alone. Copying takes
 * copiable values only (CR 707.2), so Lazav keeps his counters, his tapped-and-attacking state and
 * any Auras, and reverts at end of turn.
 *
 * The pool is filtered to creature cards *before* the prompt rather than after the pick: the card
 * says "a creature card exiled with it", so a noncreature card in the pile was never a legal
 * choice and shouldn't be offered.
 */
val LazavWearerOfFaces = card("Lazav, Wearer of Faces") {
    manaCost = "{U}{B}"
    colorIdentity = "UB"
    typeLine = "Legendary Creature — Shapeshifter Detective"
    oracleText = "Whenever Lazav attacks, exile target card from a graveyard, then investigate. " +
        "(Create a Clue token. It's an artifact with \"{2}, Sacrifice this token: Draw a card.\")\n" +
        "Whenever you sacrifice a Clue, you may have Lazav become a copy of a creature card " +
        "exiled with it until end of turn."
    power = 2
    toughness = 3

    triggeredAbility {
        trigger = Triggers.Attacks
        val graveyardCard = target("target card from a graveyard", Targets.CardInGraveyard)
        effect = Effects.Composite(
            Effects.Move(graveyardCard, Zone.EXILE, linkToSource = true),
            Effects.Investigate(),
        )
        description = "Whenever Lazav attacks, exile target card from a graveyard, then investigate."
    }

    triggeredAbility {
        trigger = Triggers.YouSacrificeA(GameObjectFilter.Artifact.withSubtype("Clue"))
        effect = Effects.Pipeline {
            val exiledWithLazav = gather(CardSource.FromLinkedExile())
            val creatureCards = filter(
                exiledWithLazav,
                GameObjectFilter.Creature,
                name = "lazavCreatureCards",
            )
            val chosen = chooseUpTo(
                1,
                from = creatureCards,
                prompt = "You may have Lazav become a copy of a creature card exiled with it",
                selectedLabel = "Become a copy",
                name = "lazavCopySource",
            )
            run(
                ConditionalEffect(
                    condition = whenMatches(chosen),
                    effect = Effects.EachPermanentBecomesCopyOfTarget(
                        target = EffectTarget.PipelineTarget(chosen.key),
                        duration = Duration.EndOfTurn,
                        affected = EffectTarget.Self,
                        sourceFromAnyZone = true,
                    ),
                )
            )
        }
        description = "Whenever you sacrifice a Clue, you may have Lazav become a copy of a " +
            "creature card exiled with it until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "216"
        artist = "Wisnu Tan"
        imageUri = "https://cards.scryfall.io/normal/front/c/c/cc264d1d-689d-41ac-b624-3fc7bb890e58.jpg?1783912843"

        ruling(
            "2024-02-02",
            "If the target of Lazav, Wearer of Faces's first ability is illegal as the ability " +
                "tries to resolve, it won't resolve and none of its effects will happen. You " +
                "won't investigate."
        )
        ruling(
            "2024-02-02",
            "Some abilities trigger \"whenever you sacrifice a Clue\". Those abilities trigger " +
                "whenever you sacrifice a Clue for any reason, not just to activate a Clue's " +
                "activated ability."
        )
        ruling(
            "2024-02-02",
            "If an effect refers to a Clue, it means any Clue artifact, not just a Clue artifact " +
                "token. For example, you can sacrifice Wrench to pay for Alquist Proft, Master " +
                "Sleuth's activated ability."
        )
        ruling(
            "2024-02-02",
            "You can't sacrifice a Clue to pay multiple costs. For example, you can't sacrifice a " +
                "Clue token to activate its own ability and also to activate Alquist Proft, " +
                "Master Sleuth's ability."
        )
    }
}
