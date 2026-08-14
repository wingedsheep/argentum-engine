package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantHexproofToController
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Captain America, Super-Soldier — Marvel Super Heroes #9 (mythic)
 * {1}{W}{W} · Legendary Creature — Human Soldier Hero · 3/2
 *
 * First strike
 * Captain America enters with a shield counter on him.
 * As long as Captain America has a shield counter on him, you and other Heroes you control have
 * hexproof.
 *
 * Implementation notes:
 * - The shield counter is the engine's CR 122.1c counter: it carries its own replacement +
 *   prevention pair ("if he would be destroyed as the result of an effect, remove a shield counter
 *   instead"; "if damage would be dealt to him, prevent it and remove a shield counter"), wired at
 *   the damage and destroy chokepoints — see `rules-engine/core/ShieldCounterHelpers.kt`. Nothing
 *   card-side is needed to make the reminder text work; entering with one is the plain
 *   [EntersWithCounters] as-enters replacement.
 * - **He does not protect himself.** "you and other Heroes" excludes Captain America, so the grant
 *   is a [GroupFilter] with `excludeSelf = true` — he stays targetable while shielding the rest of
 *   the team, which is the whole tension of the card.
 * - The two halves of the grant are separate statics because they address different kinds of
 *   object: [GrantHexproofToController] covers the *player* ("you"), [GrantKeyword] covers the
 *   permanents. Both are wrapped in the same [ConditionalStaticAbility] over
 *   [Conditions.SourceHasCounter], so both switch off the instant the counter is spent — including
 *   mid-combat, since the condition is re-evaluated during projection rather than snapshotted.
 */
val CaptainAmericaSuperSoldier = card("Captain America, Super-Soldier") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Soldier Hero"
    power = 3
    toughness = 2
    oracleText = "First strike\n" +
        "Captain America enters with a shield counter on him. (If he would be dealt damage or " +
        "destroyed, remove a shield counter from him instead.)\n" +
        "As long as Captain America has a shield counter on him, you and other Heroes you " +
        "control have hexproof."

    keywords(Keyword.FIRST_STRIKE)

    replacementEffect(
        EntersWithCounters(
            counterType = CounterTypeFilter.Named(Counters.SHIELD),
            count = 1,
            selfOnly = true,
        )
    )

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantHexproofToController,
            condition = Conditions.SourceHasCounter(CounterTypeFilter.Named(Counters.SHIELD)),
        )
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(
                Keyword.HEXPROOF,
                GroupFilter(
                    GameObjectFilter.Creature.withSubtype(Subtype.HERO).youControl(),
                    excludeSelf = true,
                ),
            ),
            condition = Conditions.SourceHasCounter(CounterTypeFilter.Named(Counters.SHIELD)),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "9"
        artist = "Anna Podedworna"
        imageUri = "https://cards.scryfall.io/normal/front/3/3/33631d6c-c584-42ff-afe5-2647b5fb321f.jpg?1783902981"
    }
}
