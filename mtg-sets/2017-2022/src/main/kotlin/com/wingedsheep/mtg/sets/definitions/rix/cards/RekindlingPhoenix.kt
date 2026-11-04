package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rekindling Phoenix
 * {2}{R}{R}
 * Creature — Phoenix
 * 4/3
 * Flying
 * When this creature dies, create a 0/1 red Elemental creature token with "At the beginning of
 * your upkeep, sacrifice this token and return target card named Rekindling Phoenix from your
 * graveyard to the battlefield. It gains haste until end of turn."
 *
 * The return isn't contingent on the sacrifice: the token's trigger returns its target even if the
 * token is already gone when it resolves.
 */
val RekindlingPhoenix = card("Rekindling Phoenix") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Phoenix"
    power = 4
    toughness = 3
    oracleText = "Flying\n" +
        "When this creature dies, create a 0/1 red Elemental creature token with \"At the beginning " +
        "of your upkeep, sacrifice this token and return target card named Rekindling Phoenix from " +
        "your graveyard to the battlefield. It gains haste until end of turn.\""

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreateToken(
            power = 0,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Elemental"),
            triggeredAbilities = listOf(
                grantedTriggeredAbility {
                    trigger = Triggers.you.beginningOf(Step.UPKEEP)
                    val phoenix = target(
                        TargetFilter(
                            GameObjectFilter.Any.named("Rekindling Phoenix").ownedByYou(),
                            zone = Zone.GRAVEYARD,
                        ),
                    )
                    effect = Effects.SacrificeTarget(EffectTarget.Self) then
                        Effects.PutOntoBattlefieldFromGraveyard(phoenix) then
                        Effects.GrantKeyword(Keyword.HASTE, phoenix)
                    description = "At the beginning of your upkeep, sacrifice this token and " +
                        "return target card named Rekindling Phoenix from your graveyard to the " +
                        "battlefield. It gains haste until end of turn."
                }
            ),
            imageUri = "https://cards.scryfall.io/normal/front/b/d/bd0bf1d7-2e66-41fd-ae9a-ea86b939e7c9.jpg?1783935215",
        )
        description = "When this creature dies, create a 0/1 red Elemental creature token with " +
            "\"At the beginning of your upkeep, sacrifice this token and return target card named " +
            "Rekindling Phoenix from your graveyard to the battlefield. It gains haste until end of turn.\""
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "111"
        artist = "Jason Rainville"
        imageUri = "https://cards.scryfall.io/normal/front/d/a/daccec32-84c6-4de4-9b00-c497a6ba5de8.jpg?1783935297"
    }
}
