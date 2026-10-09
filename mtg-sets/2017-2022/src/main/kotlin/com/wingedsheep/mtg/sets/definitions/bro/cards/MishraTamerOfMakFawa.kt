package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unearthAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.GrantWard
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

val MishraTamerOfMakFawa = card("Mishra, Tamer of Mak Fawa") {
    manaCost = "{3}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Human Artificer"
    power = 4
    toughness = 4
    oracleText = "Permanents you control have \"Ward—Sacrifice a permanent.\"\nEach artifact card in your graveyard has unearth {1}{B}{R}. ({1}{B}{R}: Return the card from your graveyard to the battlefield. It gains haste. Exile it at the beginning of the next end step or if it would leave the battlefield. Unearth only as a sorcery.)"

    staticAbility {
        ability = GrantWard(
            WardCost.Sacrifice(GameObjectFilter.Permanent),
            GroupFilter(GameObjectFilter.Permanent.youControl())
        )
    }
    staticAbility {
        ability = GrantActivatedAbility(
            unearthAbility(ManaCost.parse("{1}{B}{R}")),
            GroupFilter(GameObjectFilter.Artifact.ownedByYou()),
            recipientZone = Zone.GRAVEYARD
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "217"
        artist = "Darren Tan"
        imageUri = "https://cards.scryfall.io/normal/front/5/8/585a753d-a692-474a-bc28-48dd93b73ace.jpg?1783920025"
        ruling("2022-10-14", "Despite the appearance of the reminder text, the unearth abilities that Mishra, Tamer of Mak Fawa grants are activated abilities of each individual artifact card in your graveyard. They're not activated abilities of Mishra.")
        ruling("2022-10-14", "If you activate a card's unearth ability but that card is removed from your graveyard before the ability resolves, that unearth ability will do nothing as it resolves.")
        ruling("2022-10-14", "Activating a card's unearth ability isn't the same as casting that card. The unearth ability is put on the stack, but the card is not. Spells and abilities that interact with activated abilities (such as Defabricate's second mode) will interact with unearth, but spells and abilities that interact with spells (such as Scatter Ray) will not.")
        ruling("2022-10-14", "At the beginning of the next end step, a permanent returned to the battlefield with unearth is exiled. This is a delayed triggered ability, and it can be countered by effects such as Defabricate that counter triggered abilities. If the ability is countered, the permanent will stay on the battlefield and the delayed triggered ability won't trigger again. However, the replacement effect will still exile the permanent if it eventually leaves the battlefield.")
        ruling("2022-10-14", "Unearth grants haste to the permanent that's returned to the battlefield (even if it's not a creature card). However, neither of the \"exile\" abilities is granted to that permanent. If that permanent loses all its abilities, it will still be exiled at the beginning of the next end step, and if it would leave the battlefield, it is still exiled instead.")
        ruling("2022-10-14", "If a permanent returned to the battlefield with unearth would leave the battlefield for any reason, it's exiled instead—unless the spell or ability that's causing the permanent to leave the battlefield is actually trying to exile it! In that case, it succeeds at exiling it. If that spell or ability later returns the card to the battlefield (as Static Net might, for example), the permanent card will return to the battlefield as a new object with no relation to its previous existence. The unearth effects will no longer apply to it.")
    }
}
