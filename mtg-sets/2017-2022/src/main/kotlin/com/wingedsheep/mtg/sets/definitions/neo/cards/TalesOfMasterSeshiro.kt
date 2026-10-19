package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Tales of Master Seshiro // Seshiro's Living Legacy
 * {4}{G} — Enchantment — Saga // Enchantment Creature — Snake Warrior 5/5
 *
 * Front — Tales of Master Seshiro:
 *   I, II — Put a +1/+1 counter on target creature or Vehicle you control. It gains vigilance
 *           until end of turn.
 *   III — Exile this Saga, then return it to the battlefield transformed under your control.
 *
 * Back — Seshiro's Living Legacy:
 *   Vigilance, haste
 *
 * The chapter I/II target is [GameObjectFilter.CreatureOrVehicle] (Seven-Tail Mentor), so an
 * uncrewed Vehicle is legal and gains vigilance for when it's crewed later in the turn.
 */
private val SeshirosLivingLegacy = card("Seshiro's Living Legacy") {
    manaCost = ""
    colorIndicator = "G"
    colorIdentity = "G"
    typeLine = "Enchantment Creature — Snake Warrior"
    oracleText = "Vigilance, haste"
    power = 5
    toughness = 5

    keywords(Keyword.VIGILANCE, Keyword.HASTE)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "210"
        artist = "Olena Richards"
        flavorText = "\"In times of strife, the foolish make a fist while the wise make a friend.\"\n" +
            "—First Address of Seshiro the Anointed as Broodmaster of the Orochi Colonies"
        imageUri = "https://cards.scryfall.io/normal/back/5/1/512bc867-3a86-4da2-93f0-dd76d6a6f30d.jpg?1783923845"
    }
}

private val TalesOfMasterSeshiroFront = card("Tales of Master Seshiro") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter.)\n" +
        "I, II — Put a +1/+1 counter on target creature or Vehicle you control. " +
        "It gains vigilance until end of turn.\n" +
        "III — Exile this Saga, then return it to the battlefield transformed under your control."

    for (chapter in 1..2) {
        sagaChapter(chapter) {
            val t = target(TargetFilter(GameObjectFilter.CreatureOrVehicle.youControl()))
            effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, t) then
                Effects.GrantKeyword(Keyword.VIGILANCE, t)
        }
    }

    sagaChapter(3) {
        effect = Effects.ExileAndReturnTransformed()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "210"
        artist = "Olena Richards"
        imageUri = "https://cards.scryfall.io/normal/front/5/1/512bc867-3a86-4da2-93f0-dd76d6a6f30d.jpg?1783923845"
    }
}

val TalesOfMasterSeshiro: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = TalesOfMasterSeshiroFront,
    backFace = SeshirosLivingLegacy,
)
