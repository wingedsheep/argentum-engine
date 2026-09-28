package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.MayCastFromGraveyard
import com.wingedsheep.sdk.scripting.MayPlayLandsFromGraveyard
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Wrenn and Realmbreaker — March of the Machine #217
 * {1}{G}{G} · Legendary Planeswalker — Wrenn · Starting loyalty 4
 *
 * Lands you control have "{T}: Add one mana of any color."
 * +1: Up to one target land you control becomes a 3/3 Elemental creature with vigilance, hexproof,
 *     and haste until your next turn. It's still a land.
 * −2: Mill three cards. You may put a permanent card from among the milled cards into your hand.
 * −7: You get an emblem with "You may play lands and cast permanent spells from your graveyard."
 *
 * The −7 emblem has the statics Crucible of Worlds and its kin print — [MayPlayLandsFromGraveyard]
 * plus [MayCastFromGraveyard] — as `ownedStaticAbilities`, which every graveyard play/cast scan
 * reads off the player's emblems. "Permanent spells" never includes a land (lands are played, not
 * cast), so the cast half is scoped to nonland permanent cards.
 */
val WrennAndRealmbreaker = card("Wrenn and Realmbreaker") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Planeswalker — Wrenn"
    startingLoyalty = 4
    oracleText = "Lands you control have \"{T}: Add one mana of any color.\"\n" +
        "+1: Up to one target land you control becomes a 3/3 Elemental creature with vigilance, " +
        "hexproof, and haste until your next turn. It's still a land.\n" +
        "−2: Mill three cards. You may put a permanent card from among the milled cards into your hand.\n" +
        "−7: You get an emblem with \"You may play lands and cast permanent spells from your graveyard.\""

    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                id = AbilityId.next(),
                cost = Costs.Tap,
                effect = Effects.AddAnyColorMana(1),
                isManaAbility = true,
                timing = TimingRule.ManaAbility
            ),
            filter = GroupFilter(GameObjectFilter.Land.youControl())
        )
    }

    loyaltyAbility(+1) {
        val land = target(TargetFilter(GameObjectFilter.Land.youControl()), optional = true)
        effect = Effects.BecomeCreature(
            target = land,
            power = 3,
            toughness = 3,
            keywords = setOf(Keyword.VIGILANCE, Keyword.HEXPROOF, Keyword.HASTE),
            creatureTypes = setOf("Elemental"),
            duration = Duration.UntilYourNextTurn
        )
    }

    loyaltyAbility(-2) {
        effect = Effects.Pipeline {
            val milled = gather(CardSource.TopOfLibrary(3))
            toGraveyard(milled)
            val selected = chooseUpTo(
                1,
                from = milled,
                filter = GameObjectFilter.Permanent,
                showAllCards = true,
                prompt = "You may put a permanent card into your hand",
                selectedLabel = "Put in hand",
                remainderLabel = "Leave in graveyard"
            )
            toHand(selected)
        }
    }

    loyaltyAbility(-7) {
        effect = Effects.CreatePermanentEmblem(
            ownedStaticAbilities = listOf(
                MayPlayLandsFromGraveyard,
                MayCastFromGraveyard(filter = GameObjectFilter.NonlandPermanent)
            ),
            emblemDescription = "You may play lands and cast permanent spells from your graveyard."
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "217"
        artist = "Cristi Balanescu"
        imageUri = "https://cards.scryfall.io/normal/front/6/f/6f807d91-b157-44e8-a431-49782184f876.jpg?1783916956"
        ruling("2023-04-14", "The lands you play and spells you cast from your graveyard follow the usual timing restrictions, and you must pay any costs for spells you cast.")
    }
}
