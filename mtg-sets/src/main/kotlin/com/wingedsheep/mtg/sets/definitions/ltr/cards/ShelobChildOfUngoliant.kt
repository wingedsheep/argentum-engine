package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantWard
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Shelob, Child of Ungoliant — The Lord of the Rings: Tales of Middle-earth #230
 * {4}{B}{G} · Legendary Creature — Spider Demon · Rare
 * 8/8
 *
 * Deathtouch, ward {2}
 * Other Spiders you control have deathtouch and ward {2}.
 * Whenever another creature dealt damage this turn by a Spider you controlled dies, create a token
 * that's a copy of that creature, except it's a Food artifact with "{2}, {T}, Sacrifice this token:
 * You gain 3 life," and it loses all other card types.
 *
 * Modeling:
 * - Deathtouch is an intrinsic keyword; "ward {2}" is the parameterized ward keyword ability
 *   ([KeywordAbility.ward]).
 * - The Spider anthem is two `other Spiders you control` static grants: [GrantKeyword] (deathtouch)
 *   and [GrantWard] (`{2}`), filtered by [GroupFilter.AllCreaturesYouControl] `.withSubtype("Spider").other()`.
 * - The death-tracking ability uses the observer form of the dealt-damage-by-source-dies trigger
 *   ([Triggers.creatureDealtDamageBySourceDies] with a "Spider you control" source filter). The
 *   damaging source is matched against the filter using last-known info from when it dealt the
 *   damage, so a Spider that died in the same combat still qualifies (CR 608.2h). The token copy of
 *   the dying creature is built with [Effects.CreateTokenCopyOfTarget]: `overrideCardTypes = {ARTIFACT}`
 *   makes it "a Food artifact … and it loses all other card types", `addedSubtypes = {Food}` adds the
 *   Food type, and `activatedAbilities` grants the Food sacrifice ability.
 */
val ShelobChildOfUngoliant = card("Shelob, Child of Ungoliant") {
    manaCost = "{4}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Creature — Spider Demon"
    power = 8
    toughness = 8
    oracleText = "Deathtouch, ward {2}\n" +
        "Other Spiders you control have deathtouch and ward {2}.\n" +
        "Whenever another creature dealt damage this turn by a Spider you controlled dies, create a " +
        "token that's a copy of that creature, except it's a Food artifact with \"{2}, {T}, " +
        "Sacrifice this token: You gain 3 life,\" and it loses all other card types."

    keywords(Keyword.DEATHTOUCH)
    keywordAbility(KeywordAbility.ward("{2}"))

    // "Other Spiders you control have deathtouch and ward {2}."
    val otherSpiders = GroupFilter.AllCreaturesYouControl.withSubtype("Spider").other()
    staticAbility { ability = GrantKeyword(Keyword.DEATHTOUCH, otherSpiders) }
    staticAbility { ability = GrantWard(cost = WardCost.Mana("{2}"), filter = otherSpiders) }

    // "Whenever another creature dealt damage this turn by a Spider you controlled dies, ..."
    triggeredAbility {
        trigger = Triggers.creatureDealtDamageBySourceDies(
            GameObjectFilter.Creature.youControl().withSubtype("Spider")
        )
        effect = Effects.CreateTokenCopyOfTarget(
            target = EffectTarget.TriggeringEntity,
            overrideCardTypes = setOf(CardType.ARTIFACT),
            addedSubtypes = setOf(Subtype("Food")),
            activatedAbilities = listOf(
                ActivatedAbility(
                    cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap, Costs.SacrificeSelf),
                    effect = Effects.GainLife(3)
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "230"
        artist = "Lorenzo Mastroianni"
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a14c4b29-3363-45ce-9190-0f79e1a0ef7f.jpg?1686970060"
    }
}
