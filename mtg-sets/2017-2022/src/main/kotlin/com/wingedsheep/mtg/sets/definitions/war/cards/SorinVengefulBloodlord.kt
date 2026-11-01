package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Sorin, Vengeful Bloodlord — War of the Spark #217 (canonical printing)
 * {2}{W}{B} · Legendary Planeswalker — Sorin · Starting Loyalty: 4
 *
 * During your turn, creatures and planeswalkers you control have lifelink.
 * +2: Sorin deals 1 damage to target player or planeswalker.
 * −X: Return target creature card with mana value X from your graveyard to the battlefield. That
 *     creature is a Vampire in addition to its other types.
 *
 * The static is an [Conditions.IsYourTurn]-gated lifelink grant over creatures *and* planeswalkers
 * you control (Nahiri's shape), so Sorin's own +2 gains you 1 life on your turn. The −X targets with
 * `manaValueEqualsX()` (Tamiyo, Compleated Sage's shape) and, like Liliana, Death's Majesty, the
 * Vampire rider has no duration: a [Duration.Permanent] subtype grant on the returned object.
 */
val SorinVengefulBloodlord = card("Sorin, Vengeful Bloodlord") {
    manaCost = "{2}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Planeswalker — Sorin"
    startingLoyalty = 4
    oracleText = "During your turn, creatures and planeswalkers you control have lifelink.\n" +
        "+2: Sorin deals 1 damage to target player or planeswalker.\n" +
        "−X: Return target creature card with mana value X from your graveyard to the battlefield. " +
        "That creature is a Vampire in addition to its other types."

    // During your turn, creatures and planeswalkers you control have lifelink.
    staticAbility {
        condition = Conditions.IsYourTurn
        ability = GrantKeyword(Keyword.LIFELINK, GroupFilter(GameObjectFilter.CreatureOrPlaneswalker.youControl()))
    }

    // +2: Sorin deals 1 damage to target player or planeswalker.
    loyaltyAbility(+2) {
        val victim = target(Targets.PlayerOrPlaneswalker)
        effect = Effects.DealDamage(1, victim)
        description = "Sorin deals 1 damage to target player or planeswalker."
    }

    // −X: Return target creature card with mana value X from your graveyard to the battlefield.
    //     That creature is a Vampire in addition to its other types.
    loyaltyAbilityX {
        val creatureCard = target(TargetFilter.CreatureInYourGraveyard.manaValueEqualsX())
        effect = Effects.Move(creatureCard, Zone.BATTLEFIELD) then
            Effects.AddCreatureType("Vampire", creatureCard, Duration.Permanent)
        description = "Return target creature card with mana value X from your graveyard to the " +
            "battlefield. That creature is a Vampire in addition to its other types."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "217"
        artist = "Tommy Arnold"
        imageUri = "https://cards.scryfall.io/normal/front/3/5/354fe9bd-4ec8-409c-8ce5-b29393f3d169.jpg?1783933388"
        ruling(
            "2019-05-03",
            "If Sorin leaves the battlefield after you've activated his first loyalty ability during your turn " +
                "but before it resolves, you'll still gain 1 life as that ability deals damage."
        )
        ruling("2019-05-03", "If a card in a graveyard has {X} in its mana cost, X is considered to be 0.")
    }
}
