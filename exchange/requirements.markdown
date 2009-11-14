# Requirements
The exchange must be a self contained component which will manage all market operations within the game. The market will only handle instantly settled commodities and will be responsible for accepting and rejecting orders, matching of orders, cancelling of orders, publishing market depth and settlement.

## Accounts
Player accounts will be handled by a seperate component called the universe. The universe will keep track of player's cash balances and the inventory of commodities the player owns. The exchange will need to coordinate with the universe when accepting or rejecting orders, cancelling of orders and settlement.

## Spot Trading
The exchange will handle the trading of commodities in the game world. The exchange will only support Spot Trading. 

## Orders
The supported order types are market orders and limit orders.

## Settlement
Settlement should be instantanous and will require coordination with universe to manage the accounts of the owners of both sides of the trade.

### Procedure 
1. The market will debit the cash value of the order from the buyer's cash account in universe and credit it to the seller's cash account in universe.
2. The market will transfer the commodities purchased from the seller's account into the buyer's account 

### Minimizing counterparty risk
** Dan, I need your help here. I can't think of a good way to eliminate or reduce counterparty risk.
