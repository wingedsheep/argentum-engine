package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.ReduceActivatedAbilityCost
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/** Blossoming Tortoise — Wilds of Eldraine #163. */
val BlossomingTortoise = card("Blossoming Tortoise") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Turtle"
    oracleText = "Whenever this creature enters or attacks, mill three cards, then return a land " +
        "card from your graveyard to the battlefield tapped.\n" +
        "Activated abilities of lands you control cost {1} less to activate.\n" +
        "Land creatures you control get +1/+1."
    power = 3
    toughness = 3

    val millAndReturnLand: Effect = Effects.Composite(
        GatherCardsEffect(
            source = CardSource.TopOfLibrary(DynamicAmount.Fixed(3), Player.You),
            storeAs = "tortoiseMilled",
        ),
        MoveCollectionEffect(
            from = "tortoiseMilled",
            destination = CardDestination.ToZone(Zone.GRAVEYARD, Player.You),
        ),
        GatherCardsEffect(
            source = CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.Land),
            storeAs = "tortoiseLands",
        ),
        SelectFromCollectionEffect(
            from = "tortoiseLands",
            selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
            storeSelected = "tortoiseLandToReturn",
            showAllCards = true,
            prompt = "Return a land card from your graveyard to the battlefield tapped",
            selectedLabel = "Return tapped",
            remainderLabel = "Leave in graveyard",
        ),
        MoveCollectionEffect(
            from = "tortoiseLandToReturn",
            destination = CardDestination.ToZone(
                Zone.BATTLEFIELD,
                Player.You,
                ZonePlacement.Tapped,
            ),
        ),
    )

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = millAndReturnLand
        description = "Whenever this creature enters, mill three cards, then return a land card " +
            "from your graveyard to the battlefield tapped."
    }

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = millAndReturnLand
        description = "Whenever this creature attacks, mill three cards, then return a land card " +
            "from your graveyard to the battlefield tapped."
    }

    staticAbility {
        ability = ReduceActivatedAbilityCost(
            filter = GroupFilter(GameObjectFilter.Land.youControl()),
            amount = DynamicAmount.Fixed(1),
        )
    }

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(GameObjectFilter.Creature and GameObjectFilter.Land.youControl()),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "163"
        artist = "Simon Dominic"
        imageUri = "https://cards.scryfall.io/normal/front/7/8/7811a45d-6bfb-4c2a-b5a2-cccbd8cff186.jpg?1783915085"

        ruling(
            "2023-09-01",
            "Activated abilities contain a colon. Triggered abilities are unaffected by " +
                "Blossoming Tortoise's cost reduction ability.",
        )
        ruling(
            "2023-09-01",
            "Blossoming Tortoise's second ability affects only abilities of lands you control " +
                "on the battlefield, not abilities of land cards in other zones.",
        )
    }
}
