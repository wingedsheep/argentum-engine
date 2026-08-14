package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Garruk Relentless // Garruk, the Veil-Cursed — Innistrad #181
 * {3}{G} · Legendary Planeswalker — Garruk · Starting loyalty 3
 *
 * Front — Garruk Relentless:
 *   When Garruk has two or fewer loyalty counters on him, transform him.
 *   0: Garruk deals 3 damage to target creature. That creature deals damage equal to its power to him.
 *   0: Create a 2/2 green Wolf creature token.
 *
 * Back — Garruk, the Veil-Cursed:
 *   +1: Create a 1/1 black Wolf creature token with deathtouch.
 *   −1: Sacrifice a creature. If you do, search your library for a creature card, reveal it, put it
 *       into your hand, then shuffle.
 *   −3: Creatures you control gain trample and get +X/+X until end of turn, where X is the number of
 *       creature cards in your graveyard.
 *
 * Modeling notes:
 *
 *  - **The flip is a state-triggered ability (CR 603.8), not an upkeep trigger.** The 2011-09-22
 *    ruling is explicit: "Garruk Relentless's first ability is a state-triggered ability. It triggers
 *    once Garruk has two or fewer loyalty counters on him and it can't retrigger while that ability is
 *    on the stack." `stateTriggeredAbility` over [Conditions.SourceCounterCountAtMost] on the loyalty
 *    counter is exactly that, and the poller's latch is the "can't retrigger" part.
 *  - **Nothing resets the loyalty on the flip** — "you don't add or remove loyalty counters from
 *    Garruk Relentless when he transforms", so the back face deliberately declares no
 *    `startingLoyalty`. `TransformEffect` swaps only the face's characteristics, leaving the counters
 *    alone, and he lands on the back face with the one or two counters he had.
 *  - **Both front abilities cost 0 loyalty**, so activating one doesn't itself flip him — the front
 *    face's damage ability is what usually gets him there, via the creature hitting back. The
 *    once-per-turn loyalty restriction is per *permanent*, not per face, which the engine gets for
 *    free: transforming keeps the same entity, so "you can't activate a loyalty ability of Garruk
 *    Relentless and later that turn … a loyalty ability of Garruk, the Veil-Cursed" holds.
 *  - **The front damage ability is a two-way exchange, not a fight.** The second half attributes its
 *    damage to the *creature* via `damageSource`, so the creature's power (read at resolution) and its
 *    deathtouch/lifelink apply to the damage Garruk takes. Damage to a planeswalker removes loyalty,
 *    which is what usually trips the state trigger in the same resolution.
 *  - **The −1 doesn't target.** Per the ruling, "when that ability resolves, you must sacrifice a
 *    creature if you control one" — so it's [Effects.Sacrifice] against yourself, gated by
 *    [Effects.IfYouDo] with [SuccessCriterion.PermanentsSacrificed]. `Auto` can't infer a sacrifice
 *    (the graveyard's owner isn't known until the chooser picks), and `Always` would wrongly search on
 *    an empty board.
 *  - **Neither Wolf declares an `imageUri`.** Both faces make a Wolf that Innistrad and Innistrad
 *    Remastered each printed with their own illustration, so the art belongs to the set's
 *    `tokenArt`, not to the script — baking one in mints Innistrad's Wolf out of a Remastered
 *    Garruk. `TokenArtRegistry` keys off the printing the player brought and gets both right.
 *  - **The −3 counts creature cards in the graveyard at resolution** and freezes there — the ruling
 *    says the bonus doesn't change if that number changes later in the turn. A [DynamicAmount] fed to
 *    [Effects.ModifyStats] inside a `ForEachInGroup` gives both that and "only creatures you control
 *    when it resolves" (creatures that arrive later miss out), which is the second half of the ruling.
 */
private val GarrukRelentlessFront = card("Garruk Relentless") {
    manaCost = "{3}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Planeswalker — Garruk"
    startingLoyalty = 3
    oracleText = "When Garruk has two or fewer loyalty counters on him, transform him.\n" +
        "0: Garruk deals 3 damage to target creature. That creature deals damage equal to its " +
        "power to him.\n" +
        "0: Create a 2/2 green Wolf creature token."

    stateTriggeredAbility {
        condition = Conditions.SourceCounterCountAtMost(Counters.LOYALTY, 2)
        effect = TransformEffect(EffectTarget.Self)
        description = "When Garruk has two or fewer loyalty counters on him, transform him."
    }

    loyaltyAbility(0) {
        val creature = target("target creature", Targets.Creature)
        effect = Effects.Composite(
            Effects.DealDamage(3, creature),
            // "That creature deals damage equal to its power to him" — attributed to the creature,
            // so its power is read at resolution and its damage keywords apply.
            Effects.DealDamage(
                DynamicAmounts.targetPower(0),
                EffectTarget.Self,
                damageSource = creature,
            ),
        )
        description = "Garruk deals 3 damage to target creature. That creature deals damage equal " +
            "to its power to him."
    }

    loyaltyAbility(0) {
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Wolf"),
        )
        description = "Create a 2/2 green Wolf creature token."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "181"
        artist = "Eric Deschamps"
        imageUri = "https://cards.scryfall.io/normal/front/b/4/b4160322-ff40-41a4-887a-73cd6b85ae45.jpg?1783940925"

        ruling("2011-09-22", "Garruk Relentless's first ability is a state-triggered ability. It triggers once Garruk has two or fewer loyalty counters on him and it can't retrigger while that ability is on the stack.")
        ruling("2011-09-22", "You don't add or remove loyalty counters from Garruk Relentless when he transforms into Garruk, the Veil-Cursed. In most cases, he'll have one or two loyalty counters on him.")
        ruling("2011-09-22", "You can't activate a loyalty ability of Garruk Relentless and later that turn after he transforms activate a loyalty ability of Garruk, the Veil-Cursed.")
        ruling("2011-09-22", "The second ability of Garruk, the Veil-Cursed doesn't target a creature. However, when that ability resolves, you must sacrifice a creature if you control one.")
        ruling("2011-09-22", "The number of creature cards in your graveyard is counted when the third ability of Garruk, the Veil-Cursed resolves. Once the ability resolves, the bonus doesn't change if that number changes later in the turn.")
        ruling("2011-09-22", "Only creatures you control when the third ability of Garruk, the Veil-Cursed resolves will receive the bonus. Creatures that enter or that you gain control of later in the turn won't be affected.")
    }
}

private val GarrukTheVeilCursed = card("Garruk, the Veil-Cursed") {
    manaCost = ""
    colorIdentity = "BG"
    colorIndicator = "BG"
    typeLine = "Legendary Planeswalker — Garruk"
    oracleText = "+1: Create a 1/1 black Wolf creature token with deathtouch.\n" +
        "−1: Sacrifice a creature. If you do, search your library for a creature card, reveal it, " +
        "put it into your hand, then shuffle.\n" +
        "−3: Creatures you control gain trample and get +X/+X until end of turn, where X is the " +
        "number of creature cards in your graveyard."

    loyaltyAbility(+1) {
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Wolf"),
            keywords = setOf(Keyword.DEATHTOUCH),
        )
        description = "Create a 1/1 black Wolf creature token with deathtouch."
    }

    loyaltyAbility(-1) {
        effect = Effects.IfYouDo(
            action = Effects.Sacrifice(
                GameObjectFilter.Creature,
                count = 1,
                target = EffectTarget.PlayerRef(Player.You),
            ),
            ifYouDo = Patterns.Library.searchLibrary(
                filter = GameObjectFilter.Creature,
                destination = SearchDestination.HAND,
                reveal = true,
            ),
            successCriterion = SuccessCriterion.PermanentsSacrificed,
        )
        description = "Sacrifice a creature. If you do, search your library for a creature card, " +
            "reveal it, put it into your hand, then shuffle."
    }

    loyaltyAbility(-3) {
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            Effects.Composite(
                Effects.ModifyStats(
                    DynamicAmounts.creatureCardsInYourGraveyard(),
                    DynamicAmounts.creatureCardsInYourGraveyard(),
                    EffectTarget.Self,
                ),
                Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.Self),
            ),
        )
        description = "Creatures you control gain trample and get +X/+X until end of turn, where X " +
            "is the number of creature cards in your graveyard."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "181"
        artist = "Eric Deschamps"
        imageUri = "https://cards.scryfall.io/normal/back/b/4/b4160322-ff40-41a4-887a-73cd6b85ae45.jpg?1783940925"
    }
}

val GarrukRelentless: CardDefinition =
    CardDefinition.doubleFacedPermanent(GarrukRelentlessFront, GarrukTheVeilCursed)
