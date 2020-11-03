import pyro, numpy as np, torch, pyro.distributions as dist, torch.nn as nn
from torch import sqrt
import pyro.contrib.autoguide as ag
from pyro.optim import Adam
from pyro.infer import SVI, Trace_ELBO
from torch.autograd import Variable
import torch.distributions.constraints as constraints

data = dict()
data['offers'] =torch.Tensor(np.array([15,16]))

def model(data):
    param=pyro.sample("var1", dist.Uniform(20,50))
    Recruiters=pyro.sample("var2", dist.Poisson(param))
    percentile=pyro.sample("var3", dist.Uniform(0,1))
    if (percentile>0.95):
        GPA=4
        
    
    else:
        GPA=pyro.sample("var4", dist.Normal(2.75,0.5))
        
    if (GPA == 4):
        Interviews=dist.Binomial(Recruiters,0.9).sample()
        
    if (GPA<4):
        Interviews=dist.Binomial(Recruiters,0.6).sample()
        
    for n in range(1,2):
        with pyro.iarange("data"):
            pyro.sample("obs", dist.Binomial(Interviews,0.4), obs=data['offers'][n])
            
        

guide = ag.AutoDiagonalNormal(model)
pyro.clear_param_store()
optim = Adam({'lr': 0.01})
svi = SVI(model, guide, optim, loss=Trace_ELBO())
for i in range(1000):
    loss = svi.step(data)
    if ((i % 100) == 0):
        print(loss)
for name in pyro.get_param_store().get_all_param_names():
    print(name, pyro.param(name).data.numpy())
