package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tyvar Kell — Kaldheim #198 (canonical printing)
 * {2}{G}{G} Legendary Planeswalker — Tyvar, loyalty 3
 *
 * The −6 emblem's "it gains haste until end of turn" is a [Effects.GrantKeywordToSpell] with
 * `duration = Duration.EndOfTurn`: per the ruling (CR 400.7a), the change carries through to the
 * permanent the Elf spell becomes.
 */
private const val EMBLEM_TEXT =
    "Whenever you cast an Elf spell, it gains haste until end of turn and you draw two cards."

val TyvarKell = card("Tyvar Kell") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Planeswalker — Tyvar"
    startingLoyalty = 3
    oracleText = "Elves you control have \"{T}: Add {B}.\"\n" +
        "+1: Put a +1/+1 counter on up to one target Elf. Untap it. It gains deathtouch until end of turn.\n" +
        "0: Create a 1/1 green Elf Warrior creature token.\n" +
        "−6: You get an emblem with \"$EMBLEM_TEXT\""

    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                id = AbilityId.next(),
                cost = Costs.Tap,
                effect = Effects.AddMana(Color.BLACK),
                isManaAbility = true,
                timing = TimingRule.ManaAbility,
                descriptionOverride = "{T}: Add {B}."
            ),
            filter = GroupFilter(GameObjectFilter.Creature.withSubtype("Elf").youControl())
        )
    }

    loyaltyAbility(+1) {
        val elf = target(TargetFilter(GameObjectFilter.Creature.withSubtype("Elf")), optional = true)
        effect = Effects.AddCounters(counterType = CounterType.PLUS_ONE_PLUS_ONE, count = 1, target = elf) then
            Effects.Untap(elf) then
            Effects.GrantKeyword(Keyword.DEATHTOUCH, elf)
    }

    loyaltyAbility(0) {
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Elf", "Warrior")
        )
    }

    loyaltyAbility(-6) {
        effect = Effects.CreateGlobalTriggeredAbility(
            ability = grantedTriggeredAbility {
                trigger = Triggers.you.casts(GameObjectFilter.Any.withSubtype("Elf"))
                effect = Effects.GrantKeywordToSpell(
                    Keyword.HASTE,
                    EffectTarget.TriggeringEntity,
                    duration = Duration.EndOfTurn
                ) then Effects.DrawCards(2)
                description = EMBLEM_TEXT
            },
            descriptionOverride = EMBLEM_TEXT
        )
        description = "You get an emblem with \"$EMBLEM_TEXT\""
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "198"
        artist = "Chris Rallis"
        imageUri = "https://cards.scryfall.io/normal/front/8/d/8d18008f-10d2-4cdf-b4b5-7675782a8b0d.jpg?1783928203"

        ruling("2021-02-05", "Tyvar may look like an Elf, but he's not a creature with the creature type Elf, so he doesn't give himself the mana ability he gives Elves you control and he can't be the target of the first loyalty ability.")
        ruling("2021-02-05", "The first loyalty ability can target an Elf that's already untapped.")
        ruling("2021-02-05", "The emblem will cause the Elf spell to gain haste. That change carries through to the permanent that the spell becomes.")
    }
}
