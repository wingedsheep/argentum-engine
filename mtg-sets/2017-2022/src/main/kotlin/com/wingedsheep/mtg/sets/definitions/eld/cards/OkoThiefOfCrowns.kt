package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Oko, Thief of Crowns — Throne of Eldraine #197 (canonical printing)
 * {1}{G}{U} · Legendary Planeswalker — Oko · Loyalty 4
 *
 * +2: Create a Food token.
 * +1: Target artifact or creature loses all abilities and becomes a green Elk creature with base
 * power and toughness 3/3.
 * −5: Exchange control of target artifact or creature you control and target creature an opponent
 * controls with power 3 or less.
 *
 * The +1 is the Mercurial Transformation shape made permanent: [Effects.BecomeArtifact] with
 * `cardTypes = CREATURE` replaces every card type (the ruling: it loses other card types such as
 * artifact but keeps supertypes), replaces colors and subtypes with green Elk and removes all
 * abilities (Layer 6, timestamped — abilities gained later are kept), then
 * [Effects.SetBasePowerAndToughness] sets the Layer 7b base 3/3 so counters and pumps still apply.
 * Both last indefinitely ([Duration.Permanent]).
 */
val OkoThiefOfCrowns = card("Oko, Thief of Crowns") {
    manaCost = "{1}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Planeswalker — Oko"
    startingLoyalty = 4
    oracleText = "+2: Create a Food token. (It's an artifact with \"{2}, {T}, Sacrifice this token: " +
        "You gain 3 life.\")\n" +
        "+1: Target artifact or creature loses all abilities and becomes a green Elk creature " +
        "with base power and toughness 3/3.\n" +
        "−5: Exchange control of target artifact or creature you control and target creature " +
        "an opponent controls with power 3 or less."

    loyaltyAbility(+2) {
        effect = Effects.CreateFood()
    }

    loyaltyAbility(+1) {
        val permanent = target(TargetFilter.CreatureOrArtifact)
        effect = Effects.BecomeArtifact(
            target = permanent,
            cardTypes = setOf("CREATURE"),
            subtypes = setOf("Elk"),
            colors = setOf(Color.GREEN),
            loseAllAbilities = true,
            duration = Duration.Permanent,
        ) then Effects.SetBasePowerAndToughness(3, 3, permanent, Duration.Permanent)
    }

    loyaltyAbility(-5) {
        val yours = target(TargetFilter(GameObjectFilter.CreatureOrArtifact.youControl()))
        val theirs = target(TargetFilter.Creature.opponentControls().powerAtMost(3))
        effect = Effects.ExchangeControl(yours, theirs)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "197"
        artist = "Yongjae Choi"
        imageUri = "https://cards.scryfall.io/normal/front/3/4/3462a3d0-5552-49fa-9eb7-100960c55891.jpg?1791803810"
        ruling("2019-10-04", "Oko's second ability overwrites all colors and creature types the affected creature has. It's just a green Elk. The creature keeps any supertypes (such as legendary) it has, but loses any other card types it has (such as artifact).")
        ruling("2019-10-04", "Oko's second ability overwrites all previous effects that set the creature's base power and toughness to specific values. Any power- or toughness-setting effects that start to apply after Oko's second ability resolves will overwrite this effect.")
        ruling("2019-10-04", "The effects of Oko's second ability lasts indefinitely. It doesn't expire during the cleanup step or if you or Oko leave the game.")
        ruling("2019-10-04", "If the affected creature gains an ability after Oko's second ability resolves, it will keep that ability.")
        ruling("2019-10-04", "If either of the target permanents is an illegal target when Oko's last ability resolves, the exchange won't happen.")
        ruling("2019-10-04", "The effects of Oko's third ability lasts indefinitely. It doesn't expire during the cleanup step or if the creature you take has its power raised above 3 later.")
    }
}
