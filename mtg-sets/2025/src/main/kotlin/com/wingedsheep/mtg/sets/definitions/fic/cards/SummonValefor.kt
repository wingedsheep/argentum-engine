package com.wingedsheep.mtg.sets.definitions.fic.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Summon: Valefor — Final Fantasy Commander #42
 * {4}{U} · Enchantment Creature — Saga Drake · 5/4
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after IV.)
 * I — Sonic Wings — Each opponent chooses a creature with the greatest mana value among creatures
 *     they control. Return those creatures to their owners' hands.
 * II, III, IV — Tap up to one target creature and put a stun counter on it.
 * Flying
 *
 * Chapter I follows the ruling: each opponent in turn order chooses one of their own
 * greatest-mana-value creatures (Papalymo Totolymo's per-player `GreatestPower` choice, narrowed by
 * `GreatestManaValue` instead), then every chosen creature returns at once after the loop. An
 * opponent with no creatures contributes nothing. Chapters II–IV are Summon: Shiva's tap + stun
 * over an optional ("up to one") target creature.
 */
val SummonValefor = card("Summon: Valefor") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment Creature — Saga Drake"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after IV.)\n" +
        "I — Sonic Wings — Each opponent chooses a creature with the greatest mana value among creatures " +
        "they control. Return those creatures to their owners' hands.\n" +
        "II, III, IV — Tap up to one target creature and put a stun counter on it.\n" +
        "Flying"
    power = 5
    toughness = 4

    keywords(Keyword.FLYING)

    sagaChapter(1) {
        effect = Effects.Pipeline {
            val chosen = forEachPlayerCollecting(Player.EachOpponent) {
                val creatures = gather(GameObjectFilter.Creature, Player.You)
                val greatest = filter(creatures, CollectionFilter.GreatestManaValue)
                listOf(
                    chooseExactly(
                        1,
                        from = greatest,
                        useTargetingUI = true,
                        prompt = "Choose a creature with the greatest mana value among creatures you control " +
                            "to return to its owner's hand",
                        selectedLabel = "Return",
                    )
                )
            }.single()
            toHand(chosen)
        }
    }

    for (chapter in 2..4) {
        sagaChapter(chapter) {
            val t = target(TargetFilter.Creature, optional = true)
            effect = Effects.Tap(t) then Effects.AddCounters(CounterType.STUN, 1, t)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "42"
        artist = "SENNSU"
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e0474d99-48e5-4e60-8cd6-537a10bcfea5.jpg?1783906359"
        ruling(
            "2025-06-06",
            "While resolving Valefor's first chapter ability, the next opponent in turn order chooses a " +
                "creature with the greatest mana value among creatures they control, then each other opponent " +
                "in turn order does the same. Then the chosen creatures are returned to their owners' hands " +
                "simultaneously."
        )
        ruling(
            "2025-06-06",
            "If a creature on the battlefield has {X} in its mana cost, X is 0 for the purpose of determining " +
                "its mana value."
        )
    }
}
