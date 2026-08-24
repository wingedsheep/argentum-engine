package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.effects.CopyExceptions
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Kaya, Spirits' Justice
 * {2}{W}{B}
 * Legendary Planeswalker — Kaya
 * Starting Loyalty: 3
 *
 * Whenever one or more creatures you control and/or creature cards in your graveyard are put into
 * exile, you may choose a creature card from among them. Until end of turn, target token you
 * control becomes a copy of it, except it has flying.
 * +2: Surveil 2, then exile a card from a graveyard.
 * +1: Create a 1/1 white and black Spirit creature token with flying.
 * −2: Exile target creature you control. For each other player, exile up to one target creature
 * that player controls.
 *
 * Implementation:
 * - **The trigger** is the exile batch ([Triggers.CardsPutIntoExile]) narrowed two ways. The filter
 *   carries the ownership — `GameObjectFilter.Creature.youControl()` reads as "creatures **you
 *   control**" for the battlefield arm and "creature cards in **your** graveyard" for the graveyard
 *   arm, because the detector tests last-known control for a battlefield exit and ownership
 *   everywhere else. `includeTokens = true` is the other half: the battlefield noun here is
 *   *creatures*, not *cards*, so a token creature counts (CR 111.6 excludes tokens only from the
 *   "cards" wording, and CR 111.7 guarantees the trigger fires before the token ceases to exist).
 * - **"From among them"** is the batch the trigger captured (`triggerCaptured`). It is narrowed
 *   twice before the choice: to creature *cards* — a token that was in the batch has already been
 *   swept out of exile by CR 111.7 — and to cards still `InZone(EXILE)`, which is Kaya's own ruling
 *   that a card pulled back out of exile before this resolves can no longer be chosen.
 * - **The copy** is [Effects.EachPermanentBecomesCopyOfTarget] with the chosen card as the source
 *   (`sourceFromAnyZone = true` — it is sitting in exile) and the targeted token as `affected`,
 *   for [Duration.EndOfTurn], plus `exceptions = CopyExceptions(addedKeywords = FLYING)` for the
 *   "except it has flying" clause. Copiable values only (Rule 707.2), which is why the ruling says
 *   the token copies the card's *printed* values even if the permanent it came from was itself a
 *   copy. Declining the optional choice leaves the copy source unresolved and the effect is a
 *   no-op, which is the "you may".
 * - **+2** is Lazav, Familiar Stranger's shape verbatim: surveil 2, then gather every graveyard and
 *   exile one pick. Kaya's wording is not a "may", so the pick is [PipelineBuilder.chooseExactly] —
 *   with every graveyard empty there is nothing to choose and the step simply does nothing.
 * - **−2** is two target requirements, and CR 601.2c chooses all of them (the ruling: "You choose
 *   all targets for Kaya's last ability"). The second is the "one per player" distribution:
 *   `dynamicMaxCount = PlayerCount(EachOpponent)` says how many players are in scope,
 *   `differentControllers = true` caps it at one creature each, and `optional = true` is the "up
 *   to". ("Each other player" and "each opponent" name the same set outside team play, which is
 *   every format this set is drafted and built for; the scope is a parameter, so a future
 *   team-aware player reference is a one-word change here.)
 * - The −2 exiling your own creature is what turns the trigger on: exiling a creature you control
 *   feeds the batch, and the +2's graveyard exile does too when it takes a creature card out of
 *   *your* graveyard.
 */
private const val SPIRIT_TOKEN_IMAGE =
    "https://cards.scryfall.io/normal/front/f/4/f4588570-bde4-4c2f-8469-81a3e15fb57b.jpg?1783912607"

val KayaSpiritsJustice = card("Kaya, Spirits' Justice") {
    manaCost = "{2}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Planeswalker — Kaya"
    startingLoyalty = 3
    oracleText = "Whenever one or more creatures you control and/or creature cards in your " +
        "graveyard are put into exile, you may choose a creature card from among them. Until end " +
        "of turn, target token you control becomes a copy of it, except it has flying.\n" +
        "+2: Surveil 2, then exile a card from a graveyard.\n" +
        "+1: Create a 1/1 white and black Spirit creature token with flying.\n" +
        "−2: Exile target creature you control. For each other player, exile up to one target " +
        "creature that player controls."

    triggeredAbility {
        trigger = Triggers.CardsPutIntoExile(
            fromZones = setOf(Zone.BATTLEFIELD, Zone.GRAVEYARD),
            filter = GameObjectFilter.Creature.youControl(),
            includeTokens = true,
        )
        target("token you control", Targets.TokenYouControl)
        effect = Effects.Pipeline {
            // "from among them" — the exiled batch, narrowed to creature cards that are still in
            // exile when this resolves.
            val creatureCards = filter(
                triggerCaptured,
                GameObjectFilter.Creature.nontoken(),
                name = "kayaExiledCreatures",
            )
            val stillExiled = filter(
                creatureCards,
                CollectionFilter.InZone(Zone.EXILE),
                name = "kayaChoosable",
            )
            val chosen = chooseUpTo(
                1,
                from = stillExiled,
                prompt = "You may choose a creature card from among the exiled cards",
                selectedLabel = "Copy",
                name = "kayaCopySource",
            )
            run(
                Effects.EachPermanentBecomesCopyOfTarget(
                    target = EffectTarget.PipelineTarget(chosen.key),
                    duration = Duration.EndOfTurn,
                    affected = EffectTarget.ContextTarget(0),
                    sourceFromAnyZone = true,
                    exceptions = CopyExceptions(addedKeywords = setOf(Keyword.FLYING)),
                )
            )
        }
        description = "Whenever one or more creatures you control and/or creature cards in your " +
            "graveyard are put into exile, you may choose a creature card from among them. Until " +
            "end of turn, target token you control becomes a copy of it, except it has flying."
    }

    // +2: Surveil 2, then exile a card from a graveyard.
    loyaltyAbility(+2) {
        effect = Effects.Pipeline {
            run(Effects.Surveil(2))
            val graveyardCards = gather(
                CardSource.FromZone(Zone.GRAVEYARD, Player.Each, GameObjectFilter.Any)
            )
            val picked = chooseExactly(
                1,
                from = graveyardCards,
                useTargetingUI = true,
                prompt = "Exile a card from a graveyard",
                selectedLabel = "Exile",
                name = "kayaGraveyardExile",
            )
            exile(picked)
        }
        description = "Surveil 2, then exile a card from a graveyard."
    }

    // +1: Create a 1/1 white and black Spirit creature token with flying.
    loyaltyAbility(+1) {
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE, Color.BLACK),
            creatureTypes = setOf("Spirit"),
            keywords = setOf(Keyword.FLYING),
            name = "Spirit",
            imageUri = SPIRIT_TOKEN_IMAGE,
        )
    }

    // -2: Exile target creature you control. For each other player, exile up to one target creature
    // that player controls.
    loyaltyAbility(-2) {
        target("creature you control", TargetCreature(filter = TargetFilter.CreatureYouControl))
        target(
            "creature that player controls",
            TargetCreature(
                filter = TargetFilter.CreatureOpponentControls,
                optional = true,
                dynamicMaxCount = DynamicAmount.PlayerCount(Player.EachOpponent),
                differentControllers = true,
                id = "one target creature each other player controls",
            ),
        )
        effect = Effects.Composite(
            listOf(
                Effects.Exile(EffectTarget.ContextTarget(0)),
                Effects.Exile(EffectTarget.ContextTarget(1)),
            )
        )
        description = "Exile target creature you control. For each other player, exile up to one " +
            "target creature that player controls."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "211"
        artist = "Magali Villeneuve"
        imageUri = "https://cards.scryfall.io/normal/front/a/2/" +
            "a2827593-4951-4ba7-b73e-c27de56f2606.jpg?1783912848"

        ruling(
            "2024-02-02",
            "The target token copies the printed values of the card in exile, with the noted " +
                "exception. It doesn't matter if that card was a copy of something else when it " +
                "was on the battlefield.",
        )
        ruling(
            "2024-02-02",
            "If a creature card that was exiled is no longer in exile when Kaya, Spirits' " +
                "Justice's first ability resolves (perhaps because of Pull from Eternity or a " +
                "similar effect), you can't choose that creature card with that ability.",
        )
        ruling("2024-02-02", "You choose all targets for Kaya, Spirits' Justice's last ability.")
    }
}
