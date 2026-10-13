package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Walking Bulwark
 * {1}
 * Artifact Creature — Golem
 * 0/3
 * Defender
 * {2}: Until end of turn, target creature with defender gains haste, can attack as though it didn't
 * have defender, and assigns combat damage equal to its toughness rather than its power. Activate
 * only as a sorcery.
 *
 * The damage clause is the turn-scoped [AbilityFlag.ASSIGNS_COMBAT_DAMAGE_AS_TOUGHNESS] grant (as on
 * Bill the Pony) — unconditional, and it changes only combat damage, never the creature's power.
 */
val WalkingBulwark = card("Walking Bulwark") {
    manaCost = "{1}"
    typeLine = "Artifact Creature — Golem"
    power = 0
    toughness = 3
    oracleText = "Defender\n" +
        "{2}: Until end of turn, target creature with defender gains haste, can attack as though it didn't " +
        "have defender, and assigns combat damage equal to its toughness rather than its power. " +
        "Activate only as a sorcery."

    keywords(Keyword.DEFENDER)

    activatedAbility {
        cost = Costs.Mana("{2}")
        timing = TimingRule.SorcerySpeed
        val creature = target(TargetFilter(GameObjectFilter.Creature.withKeyword(Keyword.DEFENDER)))
        effect = Effects.GrantKeyword(Keyword.HASTE, creature) then
            Effects.CanAttackDespiteDefenderThisTurn(creature) then
            Effects.GrantKeyword(AbilityFlag.ASSIGNS_COMBAT_DAMAGE_AS_TOUGHNESS, creature)
        description = "Until end of turn, target creature with defender gains haste, can attack as though " +
            "it didn't have defender, and assigns combat damage equal to its toughness rather than its power."
    }

    metadata {
        ruling(
            "2022-09-09",
            "Walking Bulwark's last ability doesn't actually change any creature's power. It changes only the " +
                "amount of combat damage it assigns. All other rules and effects that check power or toughness " +
                "use the real values. For example, since having two creatures fight doesn't result in combat " +
                "damage, an effect that causes the affected creature to fight another creature will still use " +
                "its power to determine how much damage is dealt."
        )
        rarity = Rarity.UNCOMMON
        collectorNumber = "241"
        artist = "Jonas De Ro"
        imageUri = "https://cards.scryfall.io/normal/front/6/0/602d163c-d341-4b3b-9d3c-5d3419e9b10b.jpg?1783921264"
    }
}
