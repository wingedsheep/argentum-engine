package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Captain America, Wings of Freedom — Marvel Super Heroes #10 (rare)
 * {2}{W} · Legendary Creature — Human Soldier Hero · 3/1
 *
 * Flying, first strike, ward {1}
 * Whenever Captain America attacks, each other Hero you control gets +X/+X until end of turn,
 * where X is Captain America's toughness.
 *
 * Implementation notes:
 * - Ward is the parameterized [KeywordAbility.Ward] with a [WardCost.Mana] of `{1}`; flying and
 *   first strike are plain engine keywords.
 * - The attack trigger is the default SELF-bound [Triggers.attacks] and pumps the group with
 *   [Patterns.Group.modifyStatsForAll]. X is [EntityReference.Source]'s toughness — the group
 *   loop only rebinds `EffectTarget.Self` to the iteration entity, leaving `Source` pointing at
 *   Captain America, so every other Hero gets the *same* +X/+X read off his (projected)
 *   toughness. `excludeSelf` implements "each **other** Hero".
 * - X is snapshotted when the trigger resolves; later changes to his toughness (or his death)
 *   don't retune the already-applied bonuses, which is correct for a "+X/+X until end of turn"
 *   one-shot.
 */
val CaptainAmericaWingsOfFreedom = card("Captain America, Wings of Freedom") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Soldier Hero"
    power = 3
    toughness = 1
    oracleText = "Flying, first strike, ward {1} (Whenever this creature becomes the target of a " +
        "spell or ability an opponent controls, counter it unless that player pays {1}.)\n" +
        "Whenever Captain America attacks, each other Hero you control gets +X/+X until end of " +
        "turn, where X is Captain America's toughness."

    keywords(Keyword.FLYING, Keyword.FIRST_STRIKE)
    keywordAbility(KeywordAbility.Ward(WardCost.Mana("{1}")))

    triggeredAbility {
        trigger = Triggers.attacks()
        val sourceToughness = DynamicAmount.EntityProperty(
            EntityReference.Source,
            EntityNumericProperty.Toughness,
        )
        effect = Patterns.Group.modifyStatsForAll(
            power = sourceToughness,
            toughness = sourceToughness,
            filter = GroupFilter(
                GameObjectFilter.Creature.withSubtype(Subtype.HERO).youControl(),
                excludeSelf = true,
            ),
            duration = Duration.EndOfTurn,
        )
        description = "Whenever Captain America attacks, each other Hero you control gets +X/+X " +
            "until end of turn, where X is Captain America's toughness."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "10"
        artist = "Dan Dos Santos"
        flavorText = "\"Our worst day is only where we start from. Where we *rise* from.\"\n" +
            "—Captain America, Sam Wilson"
        imageUri = "https://cards.scryfall.io/normal/front/c/8/c87ca9ce-5034-4137-99cc-c2b28f298912.jpg?1783902978"
    }
}
