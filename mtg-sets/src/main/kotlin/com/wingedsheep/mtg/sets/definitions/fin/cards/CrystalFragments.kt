package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Crystal Fragments // Summon: Alexander
 * {W} — Artifact — Equipment
 * //  — Enchantment Creature — Saga Construct 4/3 (white color indicator)
 *
 * Front — Crystal Fragments:
 *   Equipped creature gets +1/+1.
 *   {5}{W}{W}: Exile this Equipment, then return it to the battlefield transformed under its
 *   owner's control. Activate only as a sorcery.
 *   Equip {1}
 *
 * Back — Summon: Alexander (Saga creature):
 *   (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 *   I, II — Prevent all damage that would be dealt to creatures you control this turn.
 *   III — Tap all creatures your opponents control.
 *   Flying
 *
 * Demonstrates two engine capabilities:
 *   - Equipment ↔ Saga-creature transform via [CardDefinition.doubleFacedPermanent] + the
 *     face-agnostic [Effects.ExileAndReturnTransformed] (no creature-front assumption).
 *   - Recipient-group damage prevention via [Effects.PreventAllDamageToGroup] — "prevent all damage
 *     that would be dealt to creatures you control this turn".
 */
private val SummonAlexander = card("Summon: Alexander") {
    manaCost = ""
    colorIdentity = "W"
    typeLine = "Enchantment Creature — Saga Construct"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. " +
        "Sacrifice after III.)\n" +
        "I, II — Prevent all damage that would be dealt to creatures you control this turn.\n" +
        "III — Tap all creatures your opponents control.\n" +
        "Flying"
    power = 4
    toughness = 3
    keywords(com.wingedsheep.sdk.core.Keyword.FLYING)

    // I, II — Prevent all damage that would be dealt to creatures you control this turn.
    sagaChapter(1) {
        effect = Effects.PreventAllDamageToGroup(GroupFilter.AllCreaturesYouControl)
    }
    sagaChapter(2) {
        effect = Effects.PreventAllDamageToGroup(GroupFilter.AllCreaturesYouControl)
    }

    // III — Tap all creatures your opponents control.
    sagaChapter(3) {
        effect = Patterns.Group.tapAll(GroupFilter.AllCreaturesOpponentsControl)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "13"
        artist = "Bachzim"
        imageUri = "https://cards.scryfall.io/normal/back/5/f/5f51c853-949d-44e9-a3a2-02e1ce69a147.jpg?1748707800"
    }
}

private val CrystalFragmentsFront = card("Crystal Fragments") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+1.\n" +
        "{5}{W}{W}: Exile this Equipment, then return it to the battlefield transformed under " +
        "its owner's control. Activate only as a sorcery.\n" +
        "Equip {1}"

    // Equipped creature gets +1/+1.
    staticAbility {
        ability = ModifyStats(1, 1, Filters.EquippedCreature)
    }

    // {5}{W}{W}: Exile this Equipment, then return it transformed. Activate only as a sorcery.
    activatedAbility {
        cost = Costs.Mana("{5}{W}{W}")
        timing = TimingRule.SorcerySpeed
        effect = Effects.ExileAndReturnTransformed()
    }

    equipAbility("{1}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "13"
        artist = "Bachzim"
        imageUri = "https://cards.scryfall.io/normal/front/5/f/5f51c853-949d-44e9-a3a2-02e1ce69a147.jpg?1748707800"
    }
}

val CrystalFragments: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = CrystalFragmentsFront,
    backFace = SummonAlexander,
)
