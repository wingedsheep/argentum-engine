package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Venser, Corpse Puppet
 * {U}{B}
 * Legendary Creature — Phyrexian Zombie Wizard
 * 1/3
 *
 * Lifelink, toxic 1
 * Whenever you proliferate, choose one —
 * • If you don't control a creature named The Hollow Sentinel, create The Hollow Sentinel, a
 *   legendary 3/3 colorless Phyrexian Golem artifact creature token.
 * • Target artifact creature you control gains flying and lifelink until end of turn.
 *
 * The first mode's "if" is part of the mode's effect, not an intervening-if on the trigger: the
 * mode can always be chosen, and the control check happens as the ability resolves.
 */
val VenserCorpsePuppet = card("Venser, Corpse Puppet") {
    manaCost = "{U}{B}"
    colorIdentity = "UB"
    typeLine = "Legendary Creature — Phyrexian Zombie Wizard"
    power = 1
    toughness = 3
    oracleText = "Lifelink, toxic 1\n" +
        "Whenever you proliferate, choose one —\n" +
        "• If you don't control a creature named The Hollow Sentinel, create The Hollow Sentinel, " +
        "a legendary 3/3 colorless Phyrexian Golem artifact creature token.\n" +
        "• Target artifact creature you control gains flying and lifelink until end of turn."

    keywords(Keyword.LIFELINK)
    keywordAbility(KeywordAbility.Numeric(Keyword.TOXIC, 1))

    triggeredAbility {
        trigger = Triggers.you.proliferates()
        effect = ModalEffect.chooseOne(
            mode("Create The Hollow Sentinel if you don't control it") {
                effect = Effects.If(
                    condition = Conditions.YouControl(
                        GameObjectFilter.Creature.named("The Hollow Sentinel"),
                        negate = true
                    ),
                    then = Effects.CreateToken(
                        power = 3,
                        toughness = 3,
                        creatureTypes = setOf("Phyrexian", "Golem"),
                        name = "The Hollow Sentinel",
                        legendary = true,
                        artifactToken = true,
                        imageUri = "https://cards.scryfall.io/normal/front/5/4/54e0cbfa-a7f9-49a6-9bca-4a08f5b8da93.jpg?1783918167"
                    )
                )
            },
            mode("Target artifact creature you control gains flying and lifelink until end of turn") {
                val creature = target(TargetFilter(GameObjectFilter.ArtifactCreature.youControl()))
                effect = Effects.GrantKeyword(Keyword.FLYING, creature) then
                    Effects.GrantKeyword(Keyword.LIFELINK, creature)
            },
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "219"
        artist = "Igor Kieryluk"
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1b5b94b8-0420-40f2-b989-39cb43cff916.jpg?1783917995"
        ruling(
            "2023-02-04",
            "An ability that triggers \"Whenever you proliferate\" triggers even if you chose no " +
                "permanents or players while doing so."
        )
    }
}
