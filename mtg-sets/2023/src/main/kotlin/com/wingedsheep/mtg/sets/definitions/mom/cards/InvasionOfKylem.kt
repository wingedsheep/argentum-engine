package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Invasion of Kylem // Valor's Reach Tag Team — March of the Machine #235.
 * {2}{R}{W} · Battle — Siege · defense 5 // Sorcery
 *
 * When this Siege enters, up to two target creatures each get +2/+0 and gain vigilance and haste
 * until end of turn.
 * // Create two 3/2 red and white Warrior creature tokens with "Whenever this token and at least
 * // one other creature token attack, put a +1/+1 counter on this token."
 *
 * The back face is a sorcery, so the card is built with `doubleFacedWithSpellBack`: the Siege's
 * defeat trigger casts it transformed from exile and it resolves like any sorcery (CR 712.11a).
 * The tokens' "this token and at least one other creature token attack" qualifies the attack
 * event, so it is a `triggerRestriction` over the other attacking creature tokens — checked once,
 * as the trigger fires (CR 603.2), never rechecked on resolution.
 */
private val InvasionOfKylemFront = card("Invasion of Kylem") {
    manaCost = "{2}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Battle — Siege"
    startingDefense = 5
    oracleText = "(As a Siege enters, choose an opponent to protect it. You and others can attack " +
        "it. When it's defeated, exile it, then cast it transformed.)\n" +
        "When this Siege enters, up to two target creatures each get +2/+0 and gain vigilance " +
        "and haste until end of turn."

    triggeredAbility {
        trigger = Triggers.self.enters()
        target = TargetObject(filter = TargetFilter.Creature, count = 2, optional = true)
        effect = Effects.ForEachTarget(
            Effects.ModifyStats(2, 0, EffectTarget.ContextTarget(0)) then
                Effects.GrantKeyword(Keyword.VIGILANCE, EffectTarget.ContextTarget(0)) then
                Effects.GrantKeyword(Keyword.HASTE, EffectTarget.ContextTarget(0))
        )
        description = "When this Siege enters, up to two target creatures each get +2/+0 and " +
            "gain vigilance and haste until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "235"
        artist = "Johann Bodin"
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a7ac3a39-b6a6-4647-bb6e-e82057704f8b.jpg?1783916950"
    }
}

private val ValorsReachTagTeam = card("Valor's Reach Tag Team") {
    manaCost = ""
    colorIdentity = "RW"
    colorIndicator = "RW"
    typeLine = "Sorcery"
    oracleText = "Create two 3/2 red and white Warrior creature tokens with \"Whenever this token " +
        "and at least one other creature token attack, put a +1/+1 counter on this token.\""

    spell {
        effect = Effects.CreateToken(
            count = 2,
            power = 3,
            toughness = 2,
            colors = setOf(Color.RED, Color.WHITE),
            creatureTypes = setOf("Warrior"),
            triggeredAbilities = listOf(
                TriggeredAbility.create(
                    trigger = Triggers.self.attacks(),
                    triggerRestriction = Conditions.YouControl(
                        GameObjectFilter.Creature.token().attacking(),
                        excludeSelf = true,
                    ),
                    effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
                    descriptionOverride = "Whenever this token and at least one other creature " +
                        "token attack, put a +1/+1 counter on this token.",
                ),
            ),
            imageUri = "https://cards.scryfall.io/normal/front/a/2/a2661ea2-7ff6-4188-9d40-f18bb625ed52.jpg?1783916668",
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "235"
        artist = "Johann Bodin"
        flavorText = "\"Let's give these creeps the old one-two!\""
        imageUri = "https://cards.scryfall.io/normal/back/a/7/a7ac3a39-b6a6-4647-bb6e-e82057704f8b.jpg?1783916950"
    }
}

val InvasionOfKylem: CardDefinition = CardDefinition.doubleFacedWithSpellBack(
    frontFace = InvasionOfKylemFront,
    backFace = ValorsReachTagTeam,
)
