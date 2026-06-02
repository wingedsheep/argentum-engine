package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ChooseOptionEffect
import com.wingedsheep.sdk.scripting.effects.OptionType
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Selfless Safewright
 * {3}{G}{G}
 * Creature — Elf Warrior
 * 4/2
 *
 * Flash
 * Convoke
 * When this creature enters, choose a creature type. Other permanents you control
 * of that type gain hexproof and indestructible until end of turn.
 */
val SelflessSafewright = card("Selfless Safewright") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Warrior"
    power = 4
    toughness = 2
    oracleText = "Flash\n" +
        "Convoke (Your creatures can help cast this spell. Each creature you tap while casting this spell pays for {1} or one mana of that creature's color.)\n" +
        "When this creature enters, choose a creature type. Other permanents you control of that type gain hexproof and indestructible until end of turn."

    keywords(Keyword.FLASH, Keyword.CONVOKE)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Composite(
            listOf(
                ChooseOptionEffect(
                    optionType = OptionType.CREATURE_TYPE,
                    storeAs = "chosenCreatureType"
                ),
                Effects.ForEachInGroup(
                    filter = GroupFilter(
                        baseFilter = GameObjectFilter.Permanent.youControl(),
                        excludeSelf = true,
                        chosenSubtypeKey = "chosenCreatureType"
                    ),
                    effect = Effects.Composite(
                        listOf(
                            Effects.GrantKeyword(Keyword.HEXPROOF, EffectTarget.Self, Duration.EndOfTurn),
                            Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.Self, Duration.EndOfTurn)
                        )
                    )
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "193"
        artist = "Quintin Gleim"
        imageUri = "https://cards.scryfall.io/normal/front/a/c/ac95b1c3-9eb2-4f80-bb32-72b36817d622.jpg?1767658386"
        ruling("2025-11-17", "You choose the creature type as Selfless Safewright's last ability resolves. Once the ability starts to resolve, players can't respond to the choice or take any actions until the ability finishes resolving.")
    }
}
